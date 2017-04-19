package uk.gov.hmrc.eeitt.repositories

import uk.gov.hmrc.eeitt.model._
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationNumberStorageSpec extends UnitSpec with MongoSpecSupport {

  "when a user registers, the registration number " should {
    "be stored trimmed and in uppper case " in {
      val businessUserRegistrationsRepo = new MongoRegistrationBusinessUserRepository
      await(businessUserRegistrationsRepo.removeAll())
      val registrationUser: RegistrationBusinessUser = RegistrationBusinessUser(GroupId("g1"), RegistrationNumber(" abcdefghijklmn "), RegimeId("LT"))
      await(businessUserRegistrationsRepo.collection.insert(registrationUser))

      val inst = businessUserRegistrationsRepo.findRegistrations(GroupId("g1"), RegimeId("LT"))
      inst.head.registrationNumber.value shouldBe "ABCDEFGHIJKLMN"
    }
  }

  "when a agent registers, the arn " should {
    "be stored trimmed and in uppper case " in {
      val agentRegistrationsRepo = new MongoRegistrationAgentRepository()
      await(agentRegistrationsRepo.removeAll())
      val registrationAgent: RegistrationAgent = RegistrationAgent(GroupId("g1"), Arn(" abc "))
      await(agentRegistrationsRepo.insert(registrationAgent))

      val inst = agentRegistrationsRepo.findRegistrations(GroupId("g1"))
      inst.head.arn.value shouldBe "ABC"
    }
  }
}
