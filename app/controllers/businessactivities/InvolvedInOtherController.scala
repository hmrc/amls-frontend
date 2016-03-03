package controllers.businessactivities


import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.businessactivities.{BusinessActivities, _}
import models.businessmatching.BusinessMatching
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait InvolvedInOtherController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>

      for {
          allData <- dataCacheConnector.fetchAll
        } yield {
          allData match {
          case Some(cacheData) => {
            val businessMatching = cacheData.getEntry[BusinessMatching](BusinessMatching.key)
            cacheData.getEntry[BusinessActivities](BusinessActivities.key) match {
              case Some(BusinessActivities(Some(data),_, _, _)) =>
                Ok(views.html.involved_in_other_name(Form2[InvolvedInOther](data), edit, businessMatching))
              case _ =>
                Ok(views.html.involved_in_other_name(EmptyForm, edit, businessMatching))
            }
          }
          case _ =>  Ok(views.html.involved_in_other_name(EmptyForm, edit, None))
        }
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[InvolvedInOther](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.involved_in_other_name(f, edit, None)))
        case ValidForm(_, data) =>
          for {
            businessActivities <- dataCacheConnector.fetchDataShortLivedCache[BusinessActivities](BusinessActivities.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[BusinessActivities](BusinessActivities.key,
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