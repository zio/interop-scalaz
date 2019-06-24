# Interop Scalaz

[![CircleCI][ci-badge]][ci-url]

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

[ci-badge]: https://circleci.com/gh/zio/interop-scalaz/tree/master.svg?style=svg
[ci-url]: https://circleci.com/gh/zio/interop-scalaz/tree/master
