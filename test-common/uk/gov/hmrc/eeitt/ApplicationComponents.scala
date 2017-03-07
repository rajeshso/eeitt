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

import java.io.File
import org.scalatest.{ BeforeAndAfterAll, TestSuite }
import org.scalatestplus.play.{ BaseOneAppPerSuite, FakeApplicationFactory }
import play.api._
import uk.gov.hmrc.play.it.servicemanager.ServiceManagerClient
import uk.gov.hmrc.play.it.{ ExternalService, ExternalServiceRunner, TestId }

import scala.concurrent.duration._

trait ApplicationComponentsOnePerSuite extends BaseOneAppPerSuite with FakeApplicationFactory {
  this: TestSuite =>

  def additionalConfiguration: Map[String, Any] = Map.empty[String, Any]

  private lazy val config = Configuration.from(additionalConfiguration)

  override lazy val fakeApplication =
    new ApplicationLoader().load(context.copy(initialConfiguration = context.initialConfiguration ++ config))

  def context: play.api.ApplicationLoader.Context = {
    val classLoader = play.api.ApplicationLoader.getClass.getClassLoader
    val env = new Environment(new File("."), classLoader, Mode.Test)
    play.api.ApplicationLoader.createContext(env)
  }
}

trait ApplicationComponentsOnePerSuiteIntegration extends ApplicationComponentsOnePerSuite with BeforeAndAfterAll {
  this: TestSuite =>
  val testId = new TestId("IntegrationTest-EEITT")
  val externalServiceNames = Seq("save4later")

  protected val externalServices: Seq[ExternalService] = externalServiceNames.map(ExternalServiceRunner.runFromJar(_))
  protected val externalServicePorts = ServiceManagerClient.start(testId, externalServices, 120 seconds)

  override val additionalConfiguration = externalServicePorts.foldLeft(Map.empty[String, Any]) {
    case (acc, (serviceName, port)) =>

      Logger.debug(s"External service '$serviceName' is running on port: $port")

      val updatedMap = acc +
        (s"microservice.services.$serviceName.port" -> port) +
        (s"microservice.services.$serviceName.host" -> "localhost")

      updatedMap

  }

  override def afterAll(): Unit = {
    safelyStop("Stopping external services")(ServiceManagerClient.stop(testId, true))
  }
  def safelyStop[T](activity: String)(action: => T) {
    try {
      action
    } catch {
      case t: Throwable => Logger.error(s"An exception occurred while $activity", t)
    }
  }
}
