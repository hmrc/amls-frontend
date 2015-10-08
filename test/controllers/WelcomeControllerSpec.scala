package controllers

import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future


class WelcomeControllerSpec extends UnitSpec with ScalaFutures {
  "get" should {
    "load the welcome page" in {
      val result: Future[Result] = WelcomeController.get(FakeRequest())
      whenReady(result) { result =>
        status(result) shouldBe 200
        bodyOf(result) should include("start")
      }
    }
  }
}
