package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching._
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

trait WhatDoesYourBusinessDoController extends BaseController {
  val dataCacheConnector: DataCacheConnector

  def get(index: Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        //views.html.what_does_your_business_do(EmptyForm, BusinessActivities(Seq(AccountancyServices)), edit, index)
        //buildView(EmptyForm, edit, Ok, index)
        //Future.successful(views.html.what_does_your_business_do(EmptyForm, BusinessActivities(Seq(AccountancyServices)), edit, index))
        Future.successful(Redirect(controllers.tradingpremises.routes.SummaryController.get()))
      }
  }

  private def buildView(form: Form2[_], edit: Boolean, status: Status, index: Int)(implicit authContext: AuthContext, request: Request[_]): Future[Result] = {

    dataCacheConnector.fetchAll map { x =>
      (for {
        allData <- x
        businessMatchingData <- allData.getEntry[BusinessMatching](BusinessMatching.key)
        tradingPremisesData <- allData.getEntry[TradingPremises](TradingPremises.key) orElse Some(TradingPremises())
      } yield businessMatchingData match {
        case BusinessMatching(Some(BusinessActivities(activityList))) if (activityList.size == 1) => {
          dataCacheConnector.saveDataShortLivedCache(TradingPremises.key,
            tradingPremisesData.whatDoesYourBusinessDoAtThisAddress(WhatDoesYourBusinessDo(activityList))
          ) map (_ => SeeOther(controllers.tradingpremises.routes.SummaryController.get.url))
        }
        case BusinessMatching(Some(businessActivities)) =>
          Future.successful(status(views.html.what_does_your_business_do(form, businessActivities, edit, index)))
      }) getOrElse Future.successful(NotFound)
    } flatMap (identity)
  }

  def post(index: Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[WhatDoesYourBusinessDo](request.body) match {
        case f: InvalidForm => buildView(f, edit, BadRequest, index)
        case ValidForm(_, data) =>
          for {
            tradingPremises <- dataCacheConnector.fetchDataShortLivedCache[TradingPremises](TradingPremises.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[TradingPremises](TradingPremises.key, tradingPremises.whatDoesYourBusinessDoAtThisAddress(data))
          } yield Redirect(controllers.tradingpremises.routes.SummaryController.get())
      }
    }
  }
}

object WhatDoesYourBusinessDoController extends WhatDoesYourBusinessDoController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
