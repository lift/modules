/*
 * Copyright 2010-2011 WorldWide Conferencing, LLC
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
package scalate

import xml.NodeSeq

import common._
import util._
import http._


/**
 * A {@link LiftView} which uses a <a href="http://scalate.fusesource.org/">Scalate</a>
 * template engine to resolve a URI and render it as markup
 */
class ScalateView(engine: LiftTemplateEngine = new LiftTemplateEngine()) extends LiftView with Logger {

  /**
   * Registers this view with Lift's dispatcher
   */
  def register: Unit = {
    val scalateView: ScalateView = this

    // TODO no idea why viewDispatch doesn't work, so lets just plugin the dispatcher instead
    LiftRules.dispatch.prepend(NamedPF("Scalate Dispatch") {
      case Req(path, ext, GetRequest) if (scalateView.canRender(path, ext)) => scalateView.render(path, ext)
    })

    // TODO view dispatch doesn't seem to do anything....
/*
    LiftRules.viewDispatch.prepend(NamedPF("Scalate View") {
      case Req(path, ext, GetRequest) =>
        info("scalate viewDispatch Path: " + path + " ext: " + ext)
        Right(scalateView)
    })
*/
  }


  def dispatch: PartialFunction[String, () => Box[NodeSeq]] = {
    case v if (canLoad(v)) =>
      () => Full(engine.layoutAsNodes(v))
  }


  def canRender(path: List[String], ext: String): Boolean = {
    debug("=== attempting to find: " + path + " ext: '" + ext + "'")

    if (ext == "") {
      canLoad(createUri(path, "scaml")) || canLoad(createUri(path, "ssp"))
    }
    else {
      val uri = createUri(path, ext)
      (uri.endsWith(".ssp") || uri.endsWith(".scaml")) && canLoad(uri)
    }
  }


  def render(path: List[String], ext: String): () => Box[LiftResponse] = {
    debug("attempting to render: " + path + " extension: " + ext)

    () => {
      val uri: String = if (ext != "") createUri(path, ext) else {
        List("scaml", "ssp").map(createUri(path, _)).find(engine.canLoad(_)).get
      }
      Full(TextResponse(engine.layout(uri)))
    }
  }


  protected def createUri(path: List[String], ext: String): String = path.mkString("/") +
          (if (ext.length > 0) "." + ext else "")

  protected def canLoad(v: String): Boolean = {
    engine.canLoad(v)
  }


  case class TextResponse(text: String, headers: List[(String, String)] = Nil, code: Int = 200, contentType: String = "text/html; charset=utf-8") extends LiftResponse {
    def toResponse = {
      val bytes = text.getBytes("UTF-8")
      InMemoryResponse(bytes, ("Content-Length", bytes.length.toString) :: ("Content-Type", contentType) :: headers, Nil, code)
    }
  }
}