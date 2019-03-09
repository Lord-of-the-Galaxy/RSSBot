# RSSBot
An RSS Bot for Discord, using discord.py (rewrite version)

## Installation
Download rssbot.py
You also need to install discord.py, preferably the rewrite version. Only tested on Python 3.5+, and likely doesn't work with Python 3.4

## Configuration
Edit rssbot.py
Specifically, `delay` and `feeds`. 
Delay is the delay between updates, in seconds
Format for `feeds` is: `{"<ID>":{"link":"<link-to-rss-feed>", "channels":[<channel-ids-list>]}, "<ID2>":{"link":"<link-to-rss-feed-2>", "channels":[<channel-ids-list-for-link-2>]}}`

## Run it
`python rssbot.py`

## New features?
Think something is missing? Want a new feature? If you're a programmer, then you can simply creste a pull request. Otherwise, you can [file an issue](https://github.com/Lord-of-the-Galaxy/RSSBot/issues/new).

## Bugs?
There will may bugs as this has not been properly tested. Please report the bugs [here](https://github.com/Lord-of-the-Galaxy/RSSBot/issues/new).
