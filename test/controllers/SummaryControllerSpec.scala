package controllers

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class SummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  implicit val request = FakeRequest()

  val summaryController = new SummaryController {}

  "SummaryController" must {
        "display a page" in {
          val result = summaryController.onPageLoad

      }
    }
}

