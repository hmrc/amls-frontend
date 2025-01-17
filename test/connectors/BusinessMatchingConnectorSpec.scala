/*
 * Copyright 2024 HM Revenue & Customs
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

import config.ApplicationConfig
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.{AmlsSpec, HttpClientMocker}

class BusinessMatchingConnectorSpec extends AmlsSpec with ScalaFutures {

  val validReviewDetailsJson: String =
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

  trait Fixture { self =>

    lazy val hc: HeaderCarrier = app.injector.instanceOf[HeaderCarrier]

    val mocker = new HttpClientMocker()
    private val configuration: Configuration = Configuration.load(Environment.simple())
    private val config = new ApplicationConfig(configuration, new ServicesConfig(configuration))
    val testBusinessMatchingConnector = new BusinessMatchingConnector(mocker.httpClient, config)

    val address: BusinessMatchingAddress = BusinessMatchingAddress("1 Test Street", Some("Test Town"), None, None, None, "UK")

    val validResponseDetail: BusinessMatchingReviewDetails = BusinessMatchingReviewDetails(
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

      mocker.mockGet[Option[BusinessMatchingReviewDetails]](url"http://localhost:9923/business-customer/fetch-review-details/amls", Some(validResponseDetail))

      whenReady(testBusinessMatchingConnector.getReviewDetails) { result =>
        result mustBe Some(validResponseDetail)
      }

    }

    "return None when business matching returns 404" in new Fixture {

      mocker.mockGet[Option[BusinessMatchingReviewDetails]](url"http://localhost:9923/business-customer/fetch-review-details/amls", None)

      whenReady(testBusinessMatchingConnector.getReviewDetails) { result =>
        result mustBe None
      }
    }

    "bubble the exception when any other exception is thrown" in new Fixture {
      val ex = new Exception("Some other exception")

      mocker.mockGet(url"http://localhost:9923/business-customer/fetch-review-details/amls", ex)

      intercept[Exception] {
        await(testBusinessMatchingConnector.getReviewDetails)
      } mustBe ex
    }

  }

  "The business matching review details object" should {
    "deserialize into the object properly" in {

      Json.parse(validReviewDetailsJson).as[BusinessMatchingReviewDetails]

    }
  }

}
