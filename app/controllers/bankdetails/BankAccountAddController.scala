package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.bankdetails.BankDetails
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

trait BankAccountAddController extends RepeatingSection with BaseController {
  def get(displayGuidance : Boolean = true) = Authorised.async {
    implicit authContext => implicit request =>
      addData[BankDetails](None).map { idx =>
        if (displayGuidance) {
          Redirect(routes.WhatYouNeedController.get(idx))
        } else {
          Redirect(routes.BankAccountTypeController.get(idx, false))
        }
      }
  }
}

object BankAccountAddController extends BankAccountAddController {
  // $COVERAGE-OFF$
  override def dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
