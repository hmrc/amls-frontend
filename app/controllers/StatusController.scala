package controllers

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessmatching.BusinessMatching

import scala.concurrent.Future
import views.html.status.status


trait StatusController extends BaseController{

  private[controllers] def dataCache: DataCacheConnector

  //private val businessName: String = "Ubunchews Accountancy Services"

  def get() = Authorised.async {
    implicit authContext =>
      implicit request => dataCache.fetch[BusinessMatching](BusinessMatching.key) map {
        option =>
          val businessName = for {
            businessMatching <- option
            reviewDetails <- businessMatching.reviewDetails
          } yield reviewDetails.businessName

          Ok(status(businessName.getOrElse("Not Found")))
      }


  }

}

object StatusController extends StatusController {
  // $COVERAGE-OFF$
  override private[controllers] def dataCache: DataCacheConnector = DataCacheConnector
  override protected val authConnector = AMLSAuthConnector
}
