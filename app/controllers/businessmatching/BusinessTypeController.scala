package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.businessmatching.{BusinessType, BusinessMatching}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait BusinessTypeController extends BaseController {

  private[controllers] def dataCache: DataCacheConnector

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchDataShortLivedCache[BusinessMatching](BusinessMatching.key) map {
        option =>
          // TODO Add conditional logic here
//          val redirect = for {
//            businessMatching <- option
//            reviewDetails <- businessMatching.reviewDetails
//            businessType <- reviewDetails.businessType
//          } yield Redirect(controllers.routes.MainSummaryController.onPageLoad())
//          redirect getOrElse Ok(views.html.business_type(EmptyForm))
          Ok(views.html.business_type(EmptyForm))
      }
  }

  def post() = Authorised.async {
    implicit authContext => implicit request =>
      Form2[BusinessType](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.business_type(f)))
        case ValidForm(_, data) =>
          dataCache.fetchDataShortLivedCache[BusinessMatching](BusinessMatching.key) flatMap {
            bm =>
              // TODO: Put some stuff in a service
              val updatedDetails = for {
                businessMatching <- bm
                reviewDetails <- businessMatching.reviewDetails
              } yield {
                businessMatching.copy(
                  reviewDetails = Some(
                    reviewDetails.copy(
                      businessType = Some(data.toString)
                    )
                  )
                )
              }
             updatedDetails map {
               details =>
                 dataCache.saveDataShortLivedCache[BusinessMatching](BusinessMatching.key, updatedDetails) map {
                   _ =>
                     Redirect(controllers.routes.MainSummaryController.onPageLoad())
                 }
             } getOrElse Future.successful {
               Redirect(controllers.routes.MainSummaryController.onPageLoad())
             }
          }
      }
  }
}

object BusinessTypeController extends BusinessTypeController {
  override private[controllers] def dataCache: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}