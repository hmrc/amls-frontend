package connectors

import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.status.ConfirmationStatus
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._

class KeystoreConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object KeystoreConnector extends KeystoreConnector {
    override private[connectors] val dataCache: SessionCache = mock[SessionCache]
  }

  implicit val hc = mock[HeaderCarrier]
  val mongoKey = "BC_Business_Details"

  "optionReviewDetails" must {

    "return a `Future[Option[ReviewDetails]`" in {
      when {
        KeystoreConnector.dataCache.fetchAndGetEntry[ReviewDetails](eqTo(mongoKey))(any(), any())
      } thenReturn Future.successful(None)
      whenReady (KeystoreConnector.optionalReviewDetails) {
        result =>
          result mustBe None
      }
    }
  }

  "reviewDetails" must {

    "return a successful future when review details are found" in {

      val model = ReviewDetails(
        businessName = "",
        businessType = None,
        businessAddress = Address(
          line_1 = "",
          line_2 = "",
          line_3 = None,
          line_4 = None,
          postcode = None,
          country = Country("United Kingdom", "GB")
        ),
        safeId = ""
      )

      when {
        KeystoreConnector.dataCache.fetchAndGetEntry[ReviewDetails](eqTo(mongoKey))(any(), any())
      } thenReturn Future.successful(Some(model))
      whenReady (KeystoreConnector.reviewDetails) {
        result =>
          result mustEqual model
      }
    }

    "return a failed future when review details return `None`" in {
      when {
        KeystoreConnector.dataCache.fetchAndGetEntry[ReviewDetails](eqTo(mongoKey))(any(), any())
      } thenReturn Future.successful(None)
      whenReady (KeystoreConnector.reviewDetails.failed) {
        result =>
          result mustBe a[NotFoundException]
      }
    }
  }

  "confirmationIndicator" must {

    "return a successful future when the value is found" in {

      when {
        KeystoreConnector.dataCache.fetchAndGetEntry[ConfirmationStatus](eqTo(ConfirmationStatus.key))(any(), any())
      } thenReturn Future.successful(Some(ConfirmationStatus(Some(true))))

      whenReady(KeystoreConnector.confirmationStatus) { result =>
        result mustBe ConfirmationStatus(Some(true))
      }

    }

    "return an empty successful future when the value is not found" in {
      when {
        KeystoreConnector.dataCache.fetchAndGetEntry[ConfirmationStatus](eqTo(ConfirmationStatus.key))(any(), any())
      } thenReturn Future.successful(None)

      whenReady(KeystoreConnector.confirmationStatus) { result =>
        result mustBe ConfirmationStatus(None)
      }

    }

  }
}
