package controllers

import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController

trait SummaryController extends FrontendController {
  def onPageLoad = Action {
    implicit request =>
      Ok(views.html.summaryPage())
  }

//  def onSubmit = AuthorisedFor(AmlsRegime).async {
//    implicit user =>
//      implicit request =>
//        loginDetailsForm.bindFromRequest.fold(
//          errors => Future.successful(BadRequest(views.html.AmlsLogin(errors))),
//          details => {
//            dataCacheConnector.saveDataShortLivedCache[LoginDetails](user.user.oid,"Data",details)
//            amlsService.submitLoginDetails(details).map { response =>
//              Ok(response.json)
//            } recover {
//              case e:Throwable => throw e
//            }
//          }
//        )
//  }

}

object SummaryController extends SummaryController {

}
