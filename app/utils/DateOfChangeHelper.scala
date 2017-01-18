package utils

import config.ApplicationConfig

trait DateOfChangeHelper {
  def redirectToDateOfChange[A](a: Option[A], b: A) = ApplicationConfig.release7 && !a.contains(b)
}