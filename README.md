# sinnet-server
The server component of the Sinnet chat program.

## What is Sinnet?
Sinnet is an open-source, self-hostable chat server that bakes PGP into the core functionality.
Instead of a traditional account system, identities are backed from the keys used to sign users' messages.

### Advantages
* Integration with the existing PGP ecosystem and features (web of trust, use in trustless environments, etc).
* If an attacker were to compromise a server, accounts cannot be hijacked without raising warnings on client devices.
* Persistence of identity is easily accomplished through using the same key on multiple servers.
