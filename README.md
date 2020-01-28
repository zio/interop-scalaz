# Interop Scalaz

[![CircleCI][Badge-Circle]][Link-Circle]
[![Releases][Badge-SonatypeReleases]][Link-SonatypeReleases]
[![Snapshots][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots]

This library provides instances for several Scalaz 7.2 typeclasses.

### Example

```scala
import scalaz._, Scalaz._
import zio.interop.scalaz72._

type Database = IList[User]

def findUser(id: UserId): ZIO[Database, UserError, User] = ...
def findUsers(ids: IList[UserId]): ZIO[Database, UserError, IList[User]] = ids.traverse(findUser(_))
```

## `ZIO` parallel `Applicative` instance

Due to `Applicative` and `Monad` coherence law `ZIO`'s `Applicative` instance has to be implemented in terms of `bind` hence when composing multiple effects using `Applicative` they will be sequenced. To cope with that limitation `ZIO` tagged with `Parallel` has an `Applicative` instance which is not `Monad` and operates in parallel.

### Example

```scala
import scalaz._, Scalaz._
import zio.interop.scalaz72._

case class Dashboard(details: UserDetails, history: TransactionHistory)

def getDetails(id: UserId): ZIO[Database, UserError, UserDetails] = ...
def getHistory(id: UserId): ZIO[Database, UserError, TransactionHistory] = ...

def buildDashboard(id: UserId): ZIO[Database, UserError, Dashboard] =
  Tag.unwrap(^(par(getDetails(id)), par(getHistory(id)))(Dashboard.apply))

def par[R, E, A](io: ZIO[R, E, A]): scalaz72.ParIO[R, E, A] = Tag(io)
```

[Badge-Circle]: https://circleci.com/gh/zio/interop-scalaz/tree/master.svg?style=svg
[Badge-SonatypeReleases]: https://img.shields.io/nexus/r/https/oss.sonatype.org/dev.zio/zio-interop-scalaz_2.12.svg "Sonatype Releases"
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/oss.sonatype.org/dev.zio/zio-interop-scalaz_2.12.svg "Sonatype Snapshots"
[Link-Circle]: https://circleci.com/gh/zio/interop-scalaz/tree/master
[Link-SonatypeReleases]: https://oss.sonatype.org/content/repositories/releases/dev/zio/zio-interop-scalaz_2.12/ "Sonatype Releases"
[Link-SonatypeSnapshots]: https://oss.sonatype.org/content/repositories/snapshots/dev/zio/zio-interop-scalaz_2.12/ "Sonatype Snapshots"
