package services

import javax.inject.{Inject, Singleton}

import models.registrationprogress.{NotStarted, Section}
import play.api.mvc.Call

@Singleton
class RenewalService @Inject()() {

  def getSection = {
    Section("renewal", NotStarted, false, Call("test", "test"))
  }

}
