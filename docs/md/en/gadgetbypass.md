## IP Block Bypass (Non-Japan)

<div class="alert alert-danger" role="alert">You need not to to this since gadget server is now available in all country. (2024/12/03)</div>

Since February 2020, KanColle gadget server is blocking all non-Japanese IP addresses due to DDoS attacks, etc. Therefore, in order to play KanColle normally in countries other than Japan at this point in time, you need to bypass the above blocking.

When IP block is detected, the following notification will appear when you start the game.

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/en/bypass_required.webp"  width="480" style="max-width: 100%;"/>

##### Instruction

<img src="https://gotobrowser-docs.s3.ap-northeast-1.amazonaws.com/en/gadget_options.webp"  width="600" style="max-width: 100%;" class="mb-3"/>

1. Turn on the "Gadget Block Bypass" option under the "Gadget Block Bypass" section in Settings → Login and Connection.
2. Set the bypass method to "URL Replacement" and the endpoint server to the URL below. Be careful of typos!
3. When the "Reload Required" dialog appears, press "Reload Now" to reload the game.

| Endpoint Server URL | Notes |
| --- | --- |
| `https://kcwiki.github.io/cache/` | default |

##### Notes
- If the block is found to be lifted while the bypass is on, the bypass option is turned off automatically. To keep it on, turn off "Auto-Disable Bypass" in the same section.
- Conversely, if a block is detected while the bypass is off, the notification above is shown again.
