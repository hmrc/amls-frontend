package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, EmptyForm}
import models.bankdetails.BankAccount
import models.businessmatching.BusinessActivities
import models.tradingpremises.TradingPremises

import scala.concurrent.Future

trait BankAccountController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(index: Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BankAccount](BankAccount.key) map {
        case Some(BankAccount(accountName, account)) => Ok(views.html.bank_account(EmptyForm, edit, index))
        //Future.successful(Ok(views.html.what_you_need_BD()))
        case _ => Ok(views.html.bank_account_types(EmptyForm, edit, index))
      }
  }

  /*  def post(index: Int = 0, edit: Boolean = false) = Authorised.async {
      implicit authContext => implicit request => {
        Form2[BusinessActivities](request.body) match {

          //Future.successful(Redirect(routes.BankAccountTypeController.get()))
          case _ => Redirect (routes.WhatYouNeedController.get () )
        }
    }*/
}

object BankAccountController extends BankAccountController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
