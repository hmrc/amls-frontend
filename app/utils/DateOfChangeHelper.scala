package utils

import config.ApplicationConfig

trait DateOfChangeHelper {

  def redirectToDateOfChange[A](a: Option[A], b: A) = {
    println(ApplicationConfig.release7);
    ApplicationConfig.release7 && !a.contains(b)
  }
}