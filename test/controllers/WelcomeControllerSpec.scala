package controllers

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future


class WelcomeControllerSpec extends PlaySpec with OneServerPerSuite with ScalaFutures {
  "get" should {
    "load the welcome page" in {
      val result: Future[Result] = WelcomeController.get(FakeRequest())
      status(result) must be(OK)
      contentAsString(result) must include("start")
    }
  }
}
