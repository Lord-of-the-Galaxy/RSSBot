import discord
import asyncio
import feedparser
from bs4 import BeautifulSoup

#some constants:
delay = 600
feeds = {"A":{"link":"", "channels":[]}}

class MyClient(discord.Client):
    async def on_ready(self):
        print('Logged in as')
        print(self.user.name)
        print(self.user.id)
        print('------')
        asyncio.get_event_loop().create_task(loop())

client = MyClient()

async def new_message(ch, entry):
    print("\t\t\tChannel: {}, Entry: {}".format(ch, entry['guid']))
    category = 'Unknown'
    if 'tags' in entry:
        for tag in entry.tags:
            if tag.term != 'My Colony':
                category = tag.term
                break
    summary = BeautifulSoup(entry.summary, "lxml").text
    if len(summary) > 150:
        summary = summary[:150] + '...'
        
    await client.get_channel(ch).send(":newspaper: _{}_ | **{}**\n{}\n```{}```".format(category, entry.title, entry.link, summary))

async def handle_feed(f, fid):
    c = feeds[fid]['channels'] # channels
    guids = []
    try:
        file = open("data/feed{}.rfb".format(fid), 'r')
    except IOError:
        print('error')
    else:
        with file:
             guids = file.read().splitlines()        
    with open("data/feed{}.rfb".format(fid), 'w') as file:
        for entry in f['entries']:
            file.write("{}\n".format(entry['guid']))
            if not entry['guid'] in guids:
                print("\t\tNew: {}".format(entry['guid']))
                for ch in c:
                    await new_message(ch, entry)


async def update():
    print("Updated!")
    for fid,dat in feeds.items():
        f = feedparser.parse(dat['link'])
        print("\tFeed Title: {}".format(f['feed']['title']))
        await handle_feed(f, fid)


async def loop():
    while True:
        await update()
        await asyncio.sleep(delay)

client.run('token')

