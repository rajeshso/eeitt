package uk.gov.hmrc.eeitt.repositories

import play.api.Logger
import play.api.libs.json.Json
import reactivemongo.api.DB
import reactivemongo.api.indexes.{ Index, IndexType }
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.eeitt.model.{ Registration, RegistrationRequest }

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.ExecutionContext.Implicits.global

trait RegistrationRepository {
  def lookupRegistration(groupId: String): Future[List[Registration]]
  def check(groupId: String, regimeId: String): Future[List[Registration]]
  def addRegime(registration: Registration, regimeId: String): Future[Either[String, Unit]]
  def register(registrationRequest: RegistrationRequest): Future[Either[String, Registration]]
}

class MongoRegistrationRepository(implicit mongo: () => DB)
    extends ReactiveRepository[Registration, BSONObjectID]("registrations", mongo, Registration.oFormat) with RegistrationRepository {

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    Future.sequence(Seq(
      collection.indexesManager.ensure(Index(Seq("groupId" -> IndexType.Ascending), name = Some("groupId"), unique = true, sparse = false))
    ))
  }

  def lookupRegistration(groupId: String): Future[List[Registration]] = {
    Logger.debug(s"lookup registration with group id '$groupId' in database ${collection.db.name}")
    find(("groupId", groupId))
  }

  def check(groupId: String, regimeId: String): Future[List[Registration]] = {
    Logger.debug(s"lookup registration with group id '$groupId' and regime id '$regimeId' in database ${collection.db.name}")
    find(
      "groupId" -> groupId,
      "regimeIds" -> Json.obj("$elemMatch" -> Json.obj("$eq" -> regimeId))
    )
  }

  def addRegime(registration: Registration, regimeId: String): Future[Either[String, Unit]] = {
    val regimeIds = registration.regimeIds :+ regimeId
    val selector = Json.obj("groupId" -> registration.groupId)
    val modifier = Json.obj("$set" -> Json.obj("regimeIds" -> regimeIds))
    collection.update(selector, modifier) map {
      case r if r.ok => Right((): Unit)
      case r if r.errmsg.isDefined => Left(r.errmsg.get)
      case _ => Left(s"registration update problem for ${registration.groupId}")
    }
  }

  def register(rr: RegistrationRequest): Future[Either[String, Registration]] = {
    val registration = Registration(rr.groupId, List(rr.regimeId), rr.registrationNumber, rr.groupId)
    insert(registration) map {
      case r if r.ok => Right(registration)
      case r if r.errmsg.isDefined => Left(r.errmsg.get)
      case _ => Left(s"registration problem for ${registration.groupId}")
    }
  }

}

