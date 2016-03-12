package controllers.businessactivities


import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessactivities.{BusinessActivities, _}
import models.businessmatching.BusinessMatching
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.businessactivities._

import scala.concurrent.Future

trait InvolvedInOtherController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchAll map {
        optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
          } yield {
            (for {
              businessActivties <- cache.getEntry[BusinessActivities](BusinessActivities.key)
              involvedInOther <- businessActivties.involvedInOther
            } yield Ok(involved_in_other_name(Form2[InvolvedInOther](involvedInOther), edit, businessMatching)))
              .getOrElse (Ok(involved_in_other_name(EmptyForm, edit, businessMatching)))
          }) getOrElse Ok(involved_in_other_name(EmptyForm, edit, None))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[InvolvedInOther](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(involved_in_other_name(f, edit, None)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetch[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.save[BusinessActivities](BusinessActivities.key,
              businessActivities.involvedInOther(data)
            )
          } yield edit match {
            case true => Redirect(routes.WhatYouNeedController.get())
            case false => Redirect(routes.ExpectedBusinessTurnoverController.get())
          }
      }
    }
  }
}

object InvolvedInOtherController extends InvolvedInOtherController {
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected val authConnector: AuthConnector = AMLSAuthConnector
}