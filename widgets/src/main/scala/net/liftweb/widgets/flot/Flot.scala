/*
 * Copyright 2007-2011 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.liftweb
package widgets
package flot

import xml.{NodeSeq, Unparsed}

import common._
import util._
import Helpers._
import http.LiftRules
import http.js._
import jquery._
import JsCmds._
import JE._
import JqJE._


/**
 * Renders a flot graph using http://code.google.com/p/flot/ jQuery widget
 * <br />
 * See the sites/flotDemo webapp for examples.
 */
object Flot {
  /**
   * Registers the resources with lift (typically in boot)
   */
  def init() {
    net.liftweb.http.ResourceServer.allow({
        case "flot" :: "jquery.flot.css"          :: Nil => true
        case "flot" :: "jquery.flot.js"           :: Nil => true
        case "flot" :: "jquery.flot.navigate.js"  :: Nil => true
        case "flot" :: "jquery.flot.pie.js"       :: Nil => true
        case "flot" :: "excanvas.js"              :: Nil => true
      })
  }

  def script(xml: NodeSeq): JsCmd =
    (xml \ "script").map(x => JsRaw(x.text).cmd).foldLeft(Noop)(_ & _)

  /**
   * Renders a flot graph. Comet actors should use this version.
   */
  def render(idPlaceholder: String,
             datas: List[FlotSerie],
             options: FlotOptions,
             script: JsCmd,
             caps: FlotCapability*
  ): NodeSeq = {
    renderHead() ++ Script(_renderJs(idPlaceholder, datas, options, script, caps :_*))
  }

  /**
   * Renders a flot pie graph.
   */
  def renderPie(idPlaceholder: String,
             pie: Pie,
             script: JsCmd
  ): NodeSeq = {
    renderHead() ++ Script(_renderJs(idPlaceholder, pie, script))
  }

  def renderHead(): NodeSeq = {
    val ieExcanvasPackJs = Unparsed("<!--[if IE]><script language=\"javascript\" type=\"text/javascript\" src=\"" +
                                    urlEncode(net.liftweb.http.S.contextPath) + "/" +
                                    urlEncode(LiftRules.resourceServerPath) + "/flot/excanvas.js\"></script><![endif]-->")

    <head>
      {List("flot", "flot.navigate", "flot.pie") map(name =>
      <script type="text/javascript" src={"/" + LiftRules.resourceServerPath + "/flot/jquery." + name + ".js"}/>
      )}
    {ieExcanvasPackJs}
      <link rel="stylesheet" href={"/" + LiftRules.resourceServerPath + "/flot/jquery.flot.css"} type="text/css"/>
    </head>
  }


  def renderCss (idPlaceholder : String) = {
      JqId(idPlaceholder) ~> new JsExp with JsMember {
           def toJsCmd = "addClass(\"flot_lww\")"
         }
  }

  def renderCapability (fRender: FlotCapability => JsCmd, caps: FlotCapability *): JsCmd =
  caps.foldLeft(Noop)((js, cap) => js & fRender(cap))


  /*
   * can be used to generate AJAX response
   */

  def renderJs (
    idPlaceholder : String,
      datas : List [FlotSerie],
      options : FlotOptions,
      script: JsCmd,
      caps : FlotCapability *): JsCmd =
  datas match {
    case Nil => renderFlotHide(idPlaceholder, caps: _*)

    case _ => renderVars(idPlaceholder, datas, options) &
      renderFlotShow(idPlaceholder, datas, options, script, caps :_*)
  }

  def renderFlotHide (idPlaceholder: String, caps: FlotCapability *): JsCmd =
  JsHideId(idPlaceholder) &
  renderCapability (c => c.renderHide(), caps :_*)


  // part that belongs to jQuery "document ready" function

  def renderFlotShow (
    idPlaceholder: String,
      datas: List [FlotSerie],
      options: FlotOptions,
      script: JsCmd,
      caps: FlotCapability *): JsCmd = {

    val main = FlotInfo (idPlaceholder, datas, options)

    renderCss (idPlaceholder) &
    JsShowId(idPlaceholder) &
    renderCapability (c => c.renderShow (), caps :_*) &
    JsRaw(
      "var plot_" + idPlaceholder +
      " = jQuery.plot(jQuery(" + ("#"+idPlaceholder).encJs +
      "), datas_" + idPlaceholder +
      ", options_" + idPlaceholder + ")") &
    renderCapability (c => c.render (main), caps :_*) &
    script
  }

  def renderFlotShow (
    idPlaceholder: String,
      pie: Pie,
      script: JsCmd): JsCmd = {

    renderCss(idPlaceholder) &
    JsShowId(idPlaceholder) &
    JsRaw(
      "var plot_" + idPlaceholder +
      " = jQuery.plot(jQuery(" + ("#"+idPlaceholder).encJs +
      "), data_" + idPlaceholder +
      ", {series: {pie: {show: true}}});") &
    script
  }

  // generate Javascript inside "document ready" event

  def callPlotFunction(idPlaceholder: String): JsCmd = JsRaw("flot_plot_"+idPlaceholder+"();")

  private def _renderJs (
    idPlaceholder : String,
      datas : List [FlotSerie],
      options : FlotOptions,
      script: JsCmd,
      caps : FlotCapability*): JsCmd = {
    renderVars (idPlaceholder, datas, options) &
    Function("flot_plot_"+idPlaceholder, Nil, (datas match {
          case Nil => renderFlotHide(idPlaceholder, caps : _*)
          case _ => renderFlotShow(idPlaceholder, datas, options, script,
                                   caps : _*)
        })) &
    OnLoad(callPlotFunction(idPlaceholder))
  }

  private def _renderJs (
    idPlaceholder : String,
    pie: Pie,
    script: JsCmd): JsCmd = {
    renderVars (idPlaceholder, pie) &
    Function("flot_plot_"+idPlaceholder, Nil,
      if (pie.values.isEmpty)
        renderFlotHide(idPlaceholder)
      else
        renderFlotShow(idPlaceholder, pie, script)
    ) &
    OnLoad(callPlotFunction(idPlaceholder))
  }

  /**
   * render a data value:<br/>
   * [2, 10]
   */
  def renderOneValue (one: (Double, Double)) : JsExp =
  one match {
    case (Double.NaN, _) => JsNull
    case (_, Double.NaN) => JsNull
    case (a, b) => JsArray(a, b)
  }


  /**
   * render serie of data:<br/>
   * [2, 10], [5, 12], [11, 2]
   */
  def renderValues(values: List[(Double, Double)]): JsExp =
  JsArray(values.map(renderOneValue) :_*)

  def renderDataSerie(idPlaceholder: String)(data: (FlotSerie, Int)): JsCmd =
  JsCrVar("data_"+idPlaceholder+"_"+(data._2 + 1), renderValues(data._1.data))

  /*
   * Renders all variables that can be modified via Javascript after first page load (for example using Ajax or comet).
   */
  def renderVars (idPlaceholder : String,
                  datas: List[FlotSerie],
                  options: FlotOptions): JsCmd =
  datas match {
    case Nil => Noop

    case _ =>
      datas.zipWithIndex.map(renderDataSerie(idPlaceholder)).
      reduceLeft(_ & _) &
      JsCrVar("datas_"+idPlaceholder, renderSeries(datas, idPlaceholder)) &
      JsCrVar("options_"+idPlaceholder, options.asJsObj)
  }

  def renderVars (idPlaceholder : String,
                  pie: Pie): JsCmd = {
    case class ValAndLabel(value: Int, opLabel: Option[String]) {
      override def toString = "{" +
        (opLabel match {
          case Some(label) => "label: '" + label + "', "
          case None => ""
        }) + " data: " + value + "}"
    }
    val valsAndLabels = pie.values.zipWithIndex.map(vi => ValAndLabel(vi._1, pie.labels.map(_(vi._2))))
    JsCrVar("data_" + idPlaceholder, JsRaw(valsAndLabels.mkString("[", ",", "]")))
  }

  /**
   * render one serie:<br />
   * <br />
   * <code>
   * (
   *   label: "<name_label>"
   *   lines: { show: true, fill: true }
   *   bars: { show: true }
   *   points: { show: true }
   *   data: data_[ph]_[x] where ph is the placeholder id and {x} serie's the id
   * )
   * </code>
   */
  def renderOneSerie(data: FlotSerie, idPlaceholder: String, idSerie: Int): JsObj = {
    val info: List[Box[(String, JsExp)]] =
    List(data.label.map(v => ("label", v)),
         data.lines.map(v => ("lines", v.asJsObj)),
         data.points.map(v => ("points", v.asJsObj)),
         data.bars.map(v => ("bars", v.asJsObj)),
         data.color.map {
        case Left(c) => ("color", c)
        case Right(c) => ("color", c)
      },
         data.shadowSize.map(s => ("shadowSize", s)),
         Full(("data", JsVar("data_"+idPlaceholder + "_" + idSerie))))

    JsObj(info.flatten(_.toList) :_*)
  }

  /**
   * render all series: <br />
   * <br />
   * ( <br />
   *   label: "<name_label_1>" <br />
   *   lines:  ... <br />
   *   data: [[2, 10], [5, 12], [11, 2]] <br />
   * ), <br />
   * (<br />
   *   label: "<name_label2>"<br />
   *   data: [[2, 14], [6, 4], [11, 17]]<br />
   * )<br />
   *
   */
  def renderSeries(datas: List[FlotSerie], idPlaceholder: String): JsArray =
  JsArray(datas.zipWithIndex.map{
      case (d, idx) => renderOneSerie(d, idPlaceholder, idx + 1)
    } :_*)

}
