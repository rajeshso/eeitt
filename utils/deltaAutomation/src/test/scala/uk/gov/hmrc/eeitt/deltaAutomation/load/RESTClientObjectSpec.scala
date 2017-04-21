package uk.gov.hmrc.eeitt.deltaAutomation.load

import org.scalatest.{FlatSpec, Matchers}
import uk.gov.hmrc.eeitt.deltaAutomation.transform.BusinessUser

import scalaj.http._
/**
  * Created by rajesh on 21/04/17.
  */
class RESTClientObjectSpec extends FlatSpec with Matchers {
  "An incorrect URL" should "return an error message that there is a problem with the connection" in {
    RESTClientObject.process("dsfsd",BusinessUser) match {
      case Left(response) => fail("A HTTPResponse is unexpected")
      case Right(message) => message should be ("Connection refused (Connection refused)")
    }
  }
}
