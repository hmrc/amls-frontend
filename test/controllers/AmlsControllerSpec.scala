package controllers

import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.http.{Status => HttpStatus}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.{FakeApplication, FakeRequest}
import services.AmlsService
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}

import scala.concurrent.Future

class AmlsControllerSpec extends UnitSpec with ScalaFutures with MockitoSugar with WithFakeApplication{

  implicit val request = FakeRequest()
  val fakePostRequest = FakeRequest("POST", "/login").withFormUrlEncodedBody(
    "name" -> "test",
    "password" -> "password"
  )
  val mockAmlsService = mock[AmlsService]

  override lazy val fakeApplication = FakeApplication(additionalConfiguration = Map(
    "govuk-tax.Test.services.contact-frontend.host" -> "localhost",
    "govuk-tax.Test.services.contact-frontend.port" -> "9250"
  ))

  class MockAmlsController extends AmlsController {
     val amlsService: AmlsService = mockAmlsService
  }

  "AmlsController" must {
    "load the Sample Login page" in new MockAmlsController {
      val result = await(onPageLoad()(request))
      status(result) shouldBe HttpStatus.OK
      bodyOf(result) should include ("name")
    }

    "successfully submit to Micro Service" in new MockAmlsController  {
     implicit val request = fakePostRequest
      when(amlsService.submitLoginDetails(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(HttpStatus.OK, Some(Json.parse("""{"foo":"bar"}""")))))
      val result: Result = await(onSubmit()(request))
      status(result) shouldBe HttpStatus.OK
    }

    "fail test when Micro Service throws exception" in new MockAmlsController  {
      implicit val request = fakePostRequest
      when(amlsService.submitLoginDetails(Matchers.any())(Matchers.any())).thenReturn(Future.failed(new RuntimeException("test")))
      val result: Result = await(onSubmit()(request))
      status(result) shouldBe HttpStatus.BAD_REQUEST
    }
  }
}

