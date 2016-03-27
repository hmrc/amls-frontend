package controllers.aboutthebusiness

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.aboutthebusiness.{AboutTheBusiness, PreviouslyRegistered}
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType._
import views.html.aboutthebusiness._

import scala.concurrent.Future

trait PreviouslyRegisteredController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        response =>
          val form: Form2[PreviouslyRegistered] = (for {
            aboutTheBusiness <- response
            prevRegistered <- aboutTheBusiness.previouslyRegistered
          } yield Form2[PreviouslyRegistered](prevRegistered)).getOrElse(EmptyForm)
          Ok(previously_registered(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[PreviouslyRegistered](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(previously_registered(f, edit)))
        case ValidForm(_, data) =>
          dataCacheConnector.fetchAll map {
            optionalCache =>
              (for {
                cache <- optionalCache
                businessType <- getBusinessType(cache.getEntry[BusinessMatching](BusinessMatching.key))
                aboutTheBusiness <- cache.getEntry[AboutTheBusiness](AboutTheBusiness.key)
              } yield {
                dataCacheConnector.save[AboutTheBusiness](AboutTheBusiness.key,
                  aboutTheBusiness.previouslyRegistered(data))
                (businessType, edit) match {
                    case (UNINCORPORATED_BODY | LLP | CORPORATE_BODY | PARTNERSHIP, false) =>
                        Redirect(routes.VATRegisteredController.get(edit))
                    case (_, true) => Redirect(routes.SummaryController.get())
                    case (_, _) => Redirect(routes.ConfirmRegisteredOfficeController.get())
                  }
              }).getOrElse(Redirect(routes.ConfirmRegisteredOfficeController.get(edit)))
          }
      }
    }
  }

  def getBusinessType(matching: Option[BusinessMatching]): Option[String] = {
    matching flatMap { bm =>
      bm.reviewDetails match {
        case Some(review) => review.businessType
        case _ => None
      }
    }
  }
}

object PreviouslyRegisteredController extends PreviouslyRegisteredController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}