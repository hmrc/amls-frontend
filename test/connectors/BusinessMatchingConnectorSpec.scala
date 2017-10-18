/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import utils.AuthorisedFixture

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, NotFoundException }

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
      override val crypto = SessionCookieCryptoFilter.encrypt _
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

    "return None when business matching returns 404" in new Fixture {
      when(TestBusinessMatchingConnector.httpGet.GET[BusinessMatchingReviewDetails](any())(any(), any()))
        .thenReturn(Future.failed(new NotFoundException("The review details were not found")))

      whenReady(TestBusinessMatchingConnector.getReviewDetails) { result =>
        result mustBe None
      }
    }

    "bubble the exception when any other exception is thrown" in new Fixture {
      val ex = new Exception("Some other exception")

      when {
        TestBusinessMatchingConnector.httpGet.GET[BusinessMatchingReviewDetails](any())(any(), any())
      } thenReturn Future.failed(ex)

      intercept[Exception] {
        await(TestBusinessMatchingConnector.getReviewDetails)
      } mustBe ex
    }

  }

  "The business matching review details object" should {
    "deserialize into the object properly" in {

      Json.parse(validReviewDetailsJson).as[BusinessMatchingReviewDetails]

    }
  }

}
