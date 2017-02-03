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
