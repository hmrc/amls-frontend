package services

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import models.renewal.Renewal
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext

@Singleton
class RenewalService @Inject()(dataCache: DataCacheConnector) {

  def getSection(implicit authContext: AuthContext, headerCarrier: HeaderCarrier, ec: ExecutionContext) = {

    val notStarted = Section("renewal", NotStarted, hasChanged = false, controllers.renewal.routes.WhatYouNeedController.get())

    this.getRenewal map {
      case Some(model) if model.isComplete =>
        Section("renewal", Completed, model.hasChanged, controllers.renewal.routes.SummaryController.get())
      case Some(Renewal(None, None, None, None, _, _, _, _, _, _, _)) =>
        notStarted
      case Some(model) =>
        Section("renewal", Started, model.hasChanged, controllers.renewal.routes.WhatYouNeedController.get())
      case _ => notStarted
    }

  }

  def getRenewal(implicit authContext: AuthContext, headerCarrier: HeaderCarrier, ec: ExecutionContext) =
    dataCache.fetch[Renewal](Renewal.key)

  def updateRenewal(renewal: Renewal)(implicit authContext: AuthContext, headerCarrier: HeaderCarrier, ec: ExecutionContext) =
    dataCache.save[Renewal](Renewal.key, renewal)

}
