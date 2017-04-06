package uk.gov.hmrc.eeitt.deltaAutomation.services

import java.io.{ByteArrayOutputStream, File, FileOutputStream}
import java.util.Properties
import javax.activation.{DataHandler, FileDataSource}
import javax.mail.Session
import javax.mail.internet.{InternetAddress, MimeBodyPart, MimeMessage, MimeMultipart}

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.{Message, MessagePart, ModifyMessageRequest, WatchRequest}
import com.sun.xml.internal.bind.v2.TODO
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

  def createMessage(file: File): MimeMessage = {
    val props = new Properties()
    val session = Session.getDefaultInstance(props, null)
    val email = new MimeMessage(session)
    val mimeBodyPart = new MimeBodyPart()
    val multipart = new MimeMultipart()
    val source = new FileDataSource(file)
    mimeBodyPart.setDataHandler(new DataHandler(source))
    mimeBodyPart.setFileName(file.getName)
    email.setFrom(new InternetAddress("service_eeitt.digital_ddcw@digital.hmrc.gov.uk"))
    email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress("service_eeitt.digital_ddcw@digital.hmrc.gov.uk"))
    email.setSubject("DeltaAutomation Clean Data") //TODO
    mimeBodyPart.setContent("Please find Attached Clean deltas", "text/plain")
    multipart.addBodyPart(mimeBodyPart)
    email.setContent(multipart)
    email
  }

  def sendResult = {
    val service = getGMailService
    val buffer = new ByteArrayOutputStream()
    val messagePart = createMessage
    messagePart.writeTo(buffer)
    val bytes = buffer.toByteArray
    val encodedEmail = Base64.encodeBase64URLSafeString(bytes)
    val message = new Message
    message.setRaw(encodedEmail)
    service.users().messages().send("me", message).execute()
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
        val fileOutFile = new FileOutputStream("/home/daniel-connelly/" + fileName) //Home/pi/something/input/filename
        fileOutFile.write(fileByteArray)
        fileOutFile.close()
      }
    }
    //    markMessageAsRead(id)
  }
}
