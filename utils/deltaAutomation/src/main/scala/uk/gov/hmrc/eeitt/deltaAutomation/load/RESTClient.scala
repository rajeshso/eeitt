package uk.gov.hmrc.eeitt.deltaAutomation.load

import com.typesafe.config.{ Config, ConfigFactory }
import uk.gov.hmrc.eeitt.deltaAutomation.transform.{ AgentUser, BusinessUser, Locations, UnsupportedUser, User }

import scalaj.http._

/**
 * Created by Rajesh on 13/04/17.
 */
trait RESTClient {

  lazy val conf: Config = ConfigFactory.load()
  lazy val username = conf.getString("dryrun.user")
  lazy val password = conf.getString("dryrun.password")
  lazy val requestedWith = conf.getString("dryrun.xrequestedwith")
  lazy val agenturl = conf.getString("dryrun.url.agent")
  lazy val businessurl = conf.getString("dryrun.url.business")

  //logger.debug(s"username = ${username} password = ${password} requestedWith = ${requestedWith} agenturl = ${agenturl} businessurl = ${businessurl}")

  def dryRun(payLoadString: String, user: User, xrequestedwith: String, username: String, password: String): HttpResponse[String] = {
    user match {
      case BusinessUser => dryRun(payLoadString, businessurl, requestedWith, username, password)
      case AgentUser => dryRun(payLoadString, agenturl, requestedWith, username, password)
      case UnsupportedUser => HttpResponse[String]("The user is unsupported", 0, Map[String, IndexedSeq[String]]())
    }
  }

  def dryRun(payLoadString: String, url: String, xrequestedwith: String, username: String, password: String): HttpResponse[String] = {
    val response: HttpResponse[String] = Http(url)
      .header("Content-Type", "application/json")
      .header("Charset", "UTF-8")
      .header("x-requested-with", xrequestedwith)
      .auth(username, password)
      .postData(payLoadString.getBytes("UTF-8"))
      .option(HttpOptions.readTimeout(0)).asString
    response
  }
}

object RESTClientObject extends RESTClient {
  def process(payLoadString: String, user: User): HttpResponse[String] = dryRun(payLoadString, user, requestedWith, username, password)
}
