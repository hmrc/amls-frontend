package controllers

import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController

trait SummaryController extends FrontendController {
  def onPageLoad = Action {
    implicit request =>
      Ok(views.html.summaryPage())
  }

}

object SummaryController extends SummaryController {
}
