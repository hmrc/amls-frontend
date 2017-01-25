package controllers


import com.google.inject.{Inject, Singleton}
import play.api.http.HttpErrorHandler

//object AssetsController extends AssetsBuilder

@Singleton
class AssetsController @Inject() (errorHandler: HttpErrorHandler) extends AssetsBuilder(errorHandler)
