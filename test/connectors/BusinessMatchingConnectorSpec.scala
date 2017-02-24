package connectors

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Play
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}
import utils.AuthorisedFixture

import scala.concurrent.Future

class BusinessMatchingConnectorSpec extends PlaySpec with ScalaFutures with OneAppPerSuite {

  implicit val headerCarrier = HeaderCarrier()

  val validReviewDetailsJson =
    """
      |{
      |  "businessName": "Test Business",
      |  "businessAddress": {
      |    "line_1": "1 Test Street",
      |    "line_2": "Test Town",
      |    "country": "UK"
      |  },
      |  "sapNumber": "A number",
      |  "safeId": "An id",
      |  "isAGroup": false,
      |  "directMatch": false,
      |  "agentReferenceNumber": "agent reference",
      |  "firstName": "First Name",
      |  "lastName": "Last Name",
      |  "utr": "123456789",
      |  "identification": {
      |    "idNumber": "id",
      |    "issuingInstitution": "institution",
      |    "issuingCountryCode": "UK"
      |  },
      |  "isBusinessDetailsEditable": false
      |}
    """.stripMargin

  trait Fixture extends AuthorisedFixture { self =>

    object TestBusinessMatchingConnector extends BusinessMatchingConnector {
      override val httpGet = mock[HttpGet]
    }

    val address = BusinessMatchingAddress("1 Test Street", "Test Town", None, None, None, "UK")

    val validResponseDetail = BusinessMatchingReviewDetails(
      businessName = "Test Business",
      businessType = None,
      businessAddress = address,
      sapNumber = "A number",
      safeId = "An id",
      isAGroup = false,
      directMatch = false,
      agentReferenceNumber = Some("agent reference"),
      firstName = Some("First Name"),
      lastName = Some("Last Name"),
      utr = Some("123456789"),
      identification = Some(BusinessMatchingIdentification("id", "institution", "UK")),
      isBusinessDetailsEditable = false
    )

  }

  "The business matching connector" should {

    "get the review details" in new Fixture {

      when(TestBusinessMatchingConnector.httpGet.GET[BusinessMatchingReviewDetails](any())(any(), any()))
        .thenReturn(Future.successful(validResponseDetail))

      whenReady(TestBusinessMatchingConnector.getReviewDetails) { result =>
        result mustBe Some(validResponseDetail)
      }

    }

    "return None when no details were found, or the http stack throws an exception" in new Fixture {
      when(TestBusinessMatchingConnector.httpGet.GET[BusinessMatchingReviewDetails](any())(any(), any()))
        .thenReturn(Future.failed(new Exception("Could not find any details")))

      whenReady(TestBusinessMatchingConnector.getReviewDetails) { result =>
        result mustBe None
      }
    }

  }

  "The business matching review details object" should {
    "deserialize into the object properly" in {

      Json.parse(validReviewDetailsJson).as[BusinessMatchingReviewDetails]

    }
  }

}
