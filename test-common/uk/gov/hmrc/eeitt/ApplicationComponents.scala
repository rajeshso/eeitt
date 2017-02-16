/*
 * Copyright 2016 HM Revenue & Customs
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

package uk.gov.hmrc.eeitt

import org.scalatest.{ BeforeAndAfterAll, TestSuite }
import org.scalatestplus.play.guice.{ GuiceOneAppPerSuite, GuiceOneAppPerTest }
import play.api._
import uk.gov.hmrc.play.it.servicemanager.ServiceManagerClient
import uk.gov.hmrc.play.it.{ ExternalService, ExternalServiceRunner, TestId }

import scala.concurrent.duration._

trait ApplicationComponentsOnePerTest extends GuiceOneAppPerTest with ApplicationComponents {
  this: TestSuite =>

  override val fakeApplication = new ApplicationLoader().load(context)

  override def beforeAll() = beforeAll(fakeApplication)

  override def afterAll() = afterAll(fakeApplication)
}

trait ApplicationComponentsOnePerSuite extends GuiceOneAppPerSuite with ApplicationComponents {
  this: TestSuite =>

  override val fakeApplication = new ApplicationLoader().load(context)

  override def beforeAll() = beforeAll(fakeApplication)

  override def afterAll() = afterAll(fakeApplication)
}

trait ApplicationComponents extends BeforeAndAfterAll {
  this: TestSuite =>

  def context: ApplicationLoader.Context = {
    val classLoader = ApplicationLoader.getClass.getClassLoader
    val env = new Environment(new java.io.File("."), classLoader, Mode.Test)
    ApplicationLoader.createContext(env)
  }

  def beforeAll(a: Application) {
    super.beforeAll()
    Play.start(a)
  }

  def afterAll(a: Application) {
    super.afterAll()
    Play.stop(a)
  }

}

trait ApplicationComponentsOnePerSuiteIntergration extends ApplicationComponents {
  this: TestSuite =>
  val testId = new TestId("IntergrationTest-EEITT")
  val externalServiceNames = Seq("save4later")
  protected val externalServices: Seq[ExternalService] = externalServiceNames.map(ExternalServiceRunner.runFromJar(_))
  protected val externalServicePorts = ServiceManagerClient.start(testId, externalServices, 120 seconds)
  val configMap = externalServicePorts.foldLeft(Map.empty[String, Any]) {
    case (acc, (serviceName, port)) =>

      Logger.debug(s"External service '$serviceName' is running on port: $port")

      val updatedMap = acc +
        (s"microservice.services.$serviceName.port" -> port) +
        (s"microservice.services.$serviceName.host" -> "localhost")

      updatedMap

  }

  val config: Configuration = play.api.Configuration.from(configMap)

  val intergrationContext = context.copy(initialConfiguration = context.initialConfiguration ++ config)
  val intergrationTestApplication = new ApplicationLoader().load(intergrationContext)
  override def beforeAll() = beforeAll(intergrationTestApplication)

  override def afterAll(): Unit = {
    safelyStop("Stopping external services")(ServiceManagerClient.stop(testId, true))
    afterAll(intergrationTestApplication)
  }
  def safelyStop[T](activity: String)(action: => T) {
    try {
      action
    } catch {
      case t: Throwable => Logger.error(s"An exception occurred while $activity", t)
    }
  }
}
