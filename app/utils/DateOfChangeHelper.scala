package utils

import config.ApplicationConfig
import connectors.DataCacheConnector

trait DateOfChangeHelper {

  val dataCacheConnector: DataCacheConnector = DataCacheConnector

  def redirectToDateOfChange[A](a: Option[A], b: A) = ApplicationConfig.release7 && !a.contains(b)

}