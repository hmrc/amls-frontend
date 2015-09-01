package controllers

import play.api.test.{FakeApplication, FakeRequest}
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}
class HelloWorldSpec extends UnitSpec with WithFakeApplication{

  override lazy val fakeApplication = FakeApplication(additionalConfiguration = Map(
    "govuk-tax.Test.services.contact-frontend.host" -> "localhost",
    "govuk-tax.Test.services.contact-frontend.port" -> "9250"
  ))

  implicit val request = FakeRequest()
  val HelloWorldController= HelloWorld

  "HelloWorldController" should {
    "load the hello world page" in {
      val result = await(HelloWorldController.onPageLoad()(request))
      status(result) shouldBe 200
      bodyOf(result) should include("Hello")
    }
  }
}

