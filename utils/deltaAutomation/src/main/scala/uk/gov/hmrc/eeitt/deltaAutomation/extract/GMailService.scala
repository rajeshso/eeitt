package uk.gov.hmrc.eeitt.deltaAutomation.extract

import java.io.{ ByteArrayOutputStream, File, FileOutputStream }

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64
import com.google.api.services.gmail.model._
import com.typesafe.scalalogging.Logger
import uk.gov.hmrc.eeitt.deltaAutomation.transform.{ AgentUser, BusinessUser, UnsupportedUser, User }

import scala.collection.JavaConverters._
import scala.language.implicitConversions

object GMailService extends GMailHelper {

  val logger = Logger("GMailService")

  def onNotification(): Unit = {
    logger.info("Application Started On Notifcation from Gmail")
    val userId: String = "me"
    val messages: ListMessagesResponse = gMailService.users().messages().list(userId).execute()
    logger.debug("Gathering All Emails in account")
    messages.getMessages.asScala.foreach { x =>
      val id: String = getMessageId(x)
      if (isValidEmail(id)) {
        logger.info(s"message From Email matches Ideal credentials with id : $id")
        pullDownAttachments(id)
      }
    }
  }

  def pullDownAttachments(id: String): Unit = {
    val userId: String = "me"
    val message: Message = gMailService.users().messages().get(userId, id).execute()
    val parts: List[MessagePart] = message.getPayload.getParts.asScala.toList
    for (part <- parts) {
      logger.info(s"Gathering Attachments for the message ${message.getId}")
      if (part.getFilename != null && part.getFilename.length > 0) {
        val fileName: String = part.getFilename
        val attId: String = part.getBody.getAttachmentId
        val attachPart: MessagePartBody = gMailService.users().messages().attachments().get(userId, id, attId).execute()
        val base64Url: Base64 = new Base64(true)
        logger.info(s"Decoding Email Content")
        val fileByteArray: Array[Byte] = base64Url.decode(attachPart.getData)
        val storageLocation = new File(getPath("/Files/Input"))
        val fileOutFile: FileOutputStream = new FileOutputStream(storageLocation.getPath + "/" + fileName)
        fileOutFile.write(fileByteArray)
        logger.debug("closing FileOutputStream")
        fileOutFile.close()
        logger.info(s"One Attachment Downloaded $fileName")
      }
    }
    markMessageAsRead(id)
  }

  def sendError(): Unit = {
    val errorFile = new File(getPath("/Logs") + "/error.log")
    val auditFile = new File(getPath("/Logs") + "/audit.log")
    val buffer = new ByteArrayOutputStream()
    val mimeMessage = createDeltaMessage(errorFile, auditFile, "error")
    mimeMessage.writeTo(buffer)
    val bytes = buffer.toByteArray
    val encodedEmail = Base64.encodeBase64URLSafeString(bytes)
    val message = new Message
    message.setRaw(encodedEmail)
    gMailService.users().messages().send("me", message).execute()
  }

  def sendSuccessfulResult(affinityGroup: User): Message = {
    val logFile = new File(getPath("/Logs") + "/audit.log")
    val masterFile = {
      affinityGroup match {
        case AgentUser => new File(getPath("/Files/Output/Master") + "/MasterAgent")
        case BusinessUser => new File(getPath("/Files/Output/Master") + "/MasterBusiness")
        case UnsupportedUser => throw new Exception("The user is unrecognizable")
      }
    }
    val buffer = new ByteArrayOutputStream()
    val mimeMessage = createDeltaMessage(logFile, masterFile, "success")
    mimeMessage.writeTo(buffer)
    val bytes = buffer.toByteArray
    val encodedEmail = Base64.encodeBase64URLSafeString(bytes)
    val message = new Message
    message.setRaw(encodedEmail)
    gMailService.users().messages().send("me", message).execute()
  }
}
