package uk.gov.hmrc.eeitt.repositories

import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.{ BeforeAndAfterEach, Inspectors, LoneElement }
import uk.gov.hmrc.eeitt.model._
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class UppercaseRegistrationSpec extends UnitSpec with RegistrationRepositorySupport with BeforeAndAfterEach with ScalaFutures with LoneElement with Inspectors with IntegrationPatience {

  override protected def beforeEach(): Unit = {
    await(regRepo.removeAll())
    await(repo.removeAll())
    awaitRegistrationIndexCreation()

  }

  val repo = new MongoRegistrationAgentRepository()

  private val registrationUser: RegistrationBusinessUser = RegistrationBusinessUser(GroupId("g1"), RegistrationNumber("abcdefghijklmno"), RegimeId("LT"))
  private val registrationAgent: RegistrationAgent = RegistrationAgent(GroupId("g1"), Arn("abcdefghijklmno"))

  "when a user registers the registration Number " should {
    "be in uppper case " in {
      insertRegistration(registrationUser)

      val inst = regRepo.findRegistrations(GroupId("g1"), RegimeId("LT"))
      inst.head.registrationNumber.value shouldBe "ABCDEFGHIJKLMNO"
    }
  }

  "when a agent registers the arn " should {
    "be in uppper case " in {
      await(repo.insert(registrationAgent))

      val inst = repo.findRegistrations(GroupId("g1"))
      inst.head.arn.value shouldBe "ABCDEFGHIJKLMNO"
    }
  }
}
