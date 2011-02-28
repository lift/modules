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

import net.liftweb.sbt._
import sbt._


class LiftModulesProject(info: ProjectInfo) extends ParentProject(info) with LiftParentProject {

  // TODO: consider cross-lift build, for now set it to current project version
  val liftVersion = version.toString
  
  object LiftDependencies {
    // Lift dependencies
    lazy val lift_common = "net.liftweb" %% "lift-common" % liftVersion
    lazy val lift_actor  = "net.liftweb" %% "lift-actor"  % liftVersion
    lazy val lift_json   = "net.liftweb" %% "lift-jason"  % liftVersion
    lazy val lift_util   = "net.liftweb" %% "lift-util"   % liftVersion
    lazy val lift_webkit = "net.liftweb" %% "lift-webkit" % liftVersion
    lazy val lift_db     = "net.liftweb" %% "lift-db"     % liftVersion
    lazy val lift_mapper = "net.liftweb" %% "lift-mapper" % liftVersion
  }

  import CompileScope._
  import ProvidedScope._
  import LiftDependencies._


  // Modules projects
  // ----------------
  lazy val amqp         = modulesProject("amqp", lift_actor, amqp_client)()
  lazy val facebook     = modulesProject("facebook", lift_webkit)()
  lazy val imaging      = modulesProject("imaging", lift_util, sanselan)()
  lazy val jta          = modulesProject("jta", lift_util, scalajpa, persistence_api, transaction_api,
                                         atomikos_api, atomikos_jta, atomikos_txn, atomikos_util, hibernate_em)()
  lazy val machine      = modulesProject("machine", lift_mapper)()
  lazy val oauth        = modulesProject("oauth", lift_webkit)()
  lazy val oauth_mapper = modulesProject("oauth-mapper", lift_mapper)(oauth)
  lazy val openid       = modulesProject("openid", lift_mapper, openid4java_consumer)()
  // lazy val osgi
  lazy val paypal       = modulesProject("paypal", lift_webkit, commons_httpclient)()
  lazy val scalate      = modulesProject("scalate", lift_webkit, scalate_core, servlet_api)()
  lazy val textile      = modulesProject("textile", lift_util)()
  lazy val widgets      = modulesProject("widgets", lift_webkit, logback, log4j)()
  lazy val xmpp         = modulesProject("xmpp", lift_actor, smackx)()


  // Modules apidocs
  // ---------------
  lazy val modules_doc = project(".", "lift-modules-doc", new DefaultProject(_) with LiftDefaultDocProject)


  private def modulesProject(path: String, libs: ModuleID*)(deps: Project*) =
    project(path, "lift-" + path, new ModulesProject(_, libs: _*), deps: _*)


  // Default base
  // ------------
  class ModulesProject(info: ProjectInfo, libs: ModuleID*) extends DefaultProject(info) with LiftDefaultProject {

    override def libraryDependencies = super.libraryDependencies ++ libs

    // System properties necessary during test
    override def testAction =
      super.testAction dependsOn
      task {
        System.setProperty("textile.relax", "true")
        None
      }
  }

}
