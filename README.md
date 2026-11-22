# system-design-videos

Runnable code from the videos on [youtube.com/@rajatendudey](https://www.youtube.com/@rajatendudey).

Each episode pairs runnable teaching code and tests with a source-grounded walkthrough of a real system. The simulator is not a database implementation.

## Episodes

| # | Episode | Code |
| --- | --- | --- |
| 1 | How a distributed database hunts deadlocks with DFS | [episodes/01-distributed-deadlocks](episodes/01-distributed-deadlocks) |

## Running an episode

Each episode is zero-dependency. All you need is a JDK (11 or later):

```sh
cd episodes/01-distributed-deadlocks
sh run.sh        # compiles, runs the tests, then the end-to-end simulation
```

## License

MIT. See [LICENSE](LICENSE).
