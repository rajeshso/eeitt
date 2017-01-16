package uk.gov.hmrc.eeitt.repositories

import play.api.Logger
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.{ MultiBulkWriteResult, Upserted, WriteError }
import reactivemongo.api.indexes.{ Index, IndexType }
import reactivemongo.api.{ DB, ReadPreference }
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.eeitt.model.{ Arn, EtmpAgent }
import uk.gov.hmrc.eeitt.utils.Differ
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

trait EtmpAgentRepository {
  def findByArn(arn: Arn): Future[List[EtmpAgent]]
  def replaceAll(users: Seq[EtmpAgent]): Future[MultiBulkWriteResult]
  def report(records: Seq[EtmpAgent]): Future[JsObject]
}

class MongoEtmpAgentRepository(implicit mongo: () => DB)
    extends ReactiveRepository[EtmpAgent, BSONObjectID]("etmpAgents", mongo, Json.format[EtmpAgent])
    with EtmpAgentRepository {

  override def ensureIndexes(implicit ec: ExecutionContext) = {
    collection.indexesManager.ensure(
      Index(
        key = List("arn" -> IndexType.Ascending),
        background = true,
        sparse = false
      )
    ).map(Seq(_))
  }

  def findByArn(arn: Arn) = {
    Logger.info(s"lookup etmp agent by arn '${arn.value}' in database ${collection.db.name}")
    find("arn" -> arn)
  }

  // todo: if this method fails EEITT may fail to work...
  // todo: use a correct WriteConcern
  def replaceAll(users: Seq[EtmpAgent]): Future[MultiBulkWriteResult] = {
    removeAll().flatMap { resultOfRemoving =>
      if (resultOfRemoving.ok) {
        bulkInsert(users)
      } else {
        throw new Exception("Failed to replace users")
      }
    }
  }

  def report(newRecords: Seq[EtmpAgent]): Future[JsObject] = {
    findAll().map {
      case oldRecords =>
        val diffs = Differ.diff[EtmpAgent, Arn](oldRecords, newRecords, _.arn)
        Json.obj("added" -> diffs.added.size, "changed" -> diffs.changed.size, "deleted" -> diffs.removed.size)
    }
  }

}
