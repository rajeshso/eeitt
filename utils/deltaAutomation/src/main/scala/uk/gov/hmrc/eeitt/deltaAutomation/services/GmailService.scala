package uk.gov.hmrc.eeitt.deltaAutomation.services

import java.io.FileOutputStream

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.{ Message, ModifyMessageRequest, WatchRequest }
import uk.gov.hmrc.eeitt.deltaAutomation.services.AuthService

import scala.collection.JavaConverters._
import scala.language.implicitConversions

class GmailService {

  private val authService = new AuthService

  private def getGMailService: Gmail = {
    val credential: Credential = authService.authorise
    new Gmail.Builder(authService.HTTP_TRANSPORT, authService.JSON_FACTORY, credential)
      .setApplicationName(authService.APPLICATION_NAME)
      .build
  }

  //  def configureWatch = {
  //    val service = getGMailService
  //    val userId = "me"
  //    val request = new WatchRequest
  //    request.setLabelIds(List("INBOX").asJava)
  //
  //    service.users().watch(userId, request).execute()
  //  }
  //
  //  def getHistoryId = {
  //    val service = getGMailService
  //    val messages = service.users().execute()
  //    messages
  //  }

  def isNewFile = {
    val service = getGMailService
    val userId = "me"
    val messages = service.users().messages().list(userId).execute()
    messages.getMessages.asScala.foreach { x =>
      val id = x.getId
      val labels: List[String] = getLabels(id)
      val sender = getSender(id)
      if (labels.contains("Label_1") && labels.contains("UNREAD") && sender == "<Sharlena.Campbell@hmrc.gsi.gov.uk>") {
        println(id)
        getAttachments(id)
      }
    }
  }

  def markMessageAsRead(id: String) = {
    val service = getGMailService
    val userId = "me"
    val mods = new ModifyMessageRequest().setRemoveLabelIds(List("UNREAD").asJava)
    val message = service.users.messages().modify(userId, id, mods).execute()
  }

  def getMessageId = {
    val service = getGMailService
    val userId = "me"
    val messageList = service.users().messages().list(userId).execute()
    messageList.getMessages.get(0).getId
  }

  def getLabels(id: String): List[String] = {
    val service = getGMailService
    val userId = "me"
    val messages = service.users().messages().get(userId, id).setFormat("full").execute()
    messages.getLabelIds.asScala.toList
  }

  def getSender(id: String): String = {
    val service = getGMailService
    val userId = "me"
    val messages = service.users().messages().get(userId, id).setFormat("full").execute()
    val messageList = messages.getPayload.getHeaders.asScala
    messageList.filter(header => header.getName == "From").head.getValue
  }

  def getAttachments(id: String) = {
    val service = getGMailService
    val userId = "me"
    val message = service.users().messages().get(userId, id).execute()
    val parts = message.getPayload.getParts.asScala.toList
    for (part <- parts) {
      if (part.getFilename != null && part.getFilename.length > 0) {
        val fileName = part.getFilename
        val attId = part.getBody.getAttachmentId
        val attachPart = service.users().messages().attachments().get(userId, getMessageId, attId).execute()
        val base64Url: Base64 = new Base64(true)
        val fileByteArray = base64Url.decode(attachPart.getData)
        val fileOutFile = new FileOutputStream(fileName) //Home/pi/something/input/filename
        fileOutFile.write(fileByteArray)
        fileOutFile.close()
      }
    }
    markMessageAsRead(id)
  }
}
