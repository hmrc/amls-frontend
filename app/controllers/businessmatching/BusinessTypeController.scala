package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.businessmatching.{BusinessType, BusinessMatching}
import models.businessmatching.BusinessType._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessmatching._

import scala.concurrent.Future

trait BusinessTypeController extends BaseController {

  private[controllers] def dataCache: DataCacheConnector

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[BusinessMatching](BusinessMatching.key) map {
        option =>
          val redirect = for {
            businessMatching <- option
            reviewDetails <- businessMatching.reviewDetails
            businessType <- reviewDetails.businessType
          } yield businessType match {
            case UNINCORPORATED_BODY => Redirect(routes.TypeOfBusinessController.get())
            case LLP |CORPORATE_BODY => Redirect(routes.CompanyRegistrationNumberController.get())
            case _ => Redirect(routes.RegisterServicesController.get())
          }
          redirect getOrElse Ok(business_type(EmptyForm))
      }
  }

  def post() = Authorised.async {
    implicit authContext => implicit request =>
      Form2[BusinessType](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_type(f)))
        case ValidForm(_, data) =>
          dataCache.fetch[BusinessMatching](BusinessMatching.key) flatMap {
            bm =>
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
                 dataCache.save[BusinessMatching](BusinessMatching.key, updatedDetails) map {
                   _ =>
                     Redirect(controllers.routes.RegistrationProgressController.get())
                 }
             } getOrElse Future.successful {
               Redirect(controllers.routes.RegistrationProgressController.get())
             }
          }
      }
  }
}

object BusinessTypeController extends BusinessTypeController {
  override private[controllers] def dataCache: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}