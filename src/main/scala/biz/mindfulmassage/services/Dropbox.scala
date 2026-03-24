package biz.mindfulmassage.services

import java.io.InputStream
import java.util.Locale
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.{FileMetadata, WriteMode}

class Dropbox {

  private val conf = DbxRequestConfig.newBuilder("mm-api")
    .withUserLocaleFrom(Locale.US)
    .withAutoRetryEnabled(3)
    .build()

  private val credentials = new DbxCredential(
    "",
    0L,
    biz.mindfulmassage.conf.getString("dropbox.refresh-token"),
    biz.mindfulmassage.conf.getString("dropbox.app-key"),
    biz.mindfulmassage.conf.getString("dropbox.app-secret"))

  private val client =
    new DbxClientV2(conf, credentials)

  def download(path: String): InputStream = {
    client.files.download(path).getInputStream
  }

  def upload(path: String, inputStream: InputStream): FileMetadata = {
    client.files.uploadBuilder(path).withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream)
  }

}
