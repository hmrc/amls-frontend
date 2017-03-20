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

    dataCache.fetch[Renewal](Renewal.key) map {
      case Some(model) if model.isComplete =>
        Section("renewal", Completed, model.hasChanged, controllers.renewal.routes.SummaryController.get())
      case Some(Renewal(None, _)) =>
        notStarted
      case Some(model) =>
        Section("renewal", Started, model.hasChanged, controllers.renewal.routes.WhatYouNeedController.get())
      case _ => notStarted
    }

  }

}
