// Simulate mouse hover effects by dispatching new custom events "touchover" and "touchout"
function patchInteractionManager() {
    var proto = PIXI.interaction.InteractionManager.prototype;
    proto.update = mobileUpdate;

    function extendMethod(method, extFn) {
        var old = proto[method];
        proto[method] = function() {
            old.call(this, ...arguments);
            extFn.call(this, ...arguments);
        };
    }

    extendMethod('onPointerDown', function(displayObject, hit) {
        if (this.eventData.data)
            this.processInteractive(this.eventData, this.renderer._lastObjectRendered, this.processTouchOverOut, true);
    });

    extendMethod('onPointerUp', function(displayObject, hit) {
        if (this.eventData.data)
            this.processInteractive(this.eventData, this.renderer._lastObjectRendered, this.processTouchOverOut, true);
    });

    function mobileUpdate(deltaTime) {
        // Fixed interactionFrequency = 4ms
        this._deltaTime += deltaTime;
        if (this._deltaTime < 4)
            return;
        this._deltaTime = 0;
        if (!this.interactionDOMElement)
            return;
        if (!this.eventData || !this.eventData.data) return;
        if (this.eventData.data && (this.eventData.type == 'touchmove' || this.eventData.type == 'touchend' || this.eventData.type == 'tap'))
            this.processInteractive(this.eventData, this.renderer._lastObjectRendered, this.processTouchOverOut, true);
    }

    proto.processTouchOverOut = function(interactionEvent, displayObject, hit) {
        if (!interactionEvent.data) return;
        if (hit) {
            if (!displayObject.___over && displayObject._events.touchover) {
                // supply all button
                if (displayObject.parent._onClickAll2) return;
                // left side buttons (item store page)
                if (displayObject.parent._btns && displayObject.parent.parent._onPurchased) return;
                this._hoverObject = displayObject;
                displayObject.___over = true;
                proto.dispatchEvent(displayObject, 'touchover', interactionEvent);
            }
        } else if (displayObject.___over && displayObject._events.touchover &&
            // Only trigger "touchout" when user starts touching another object or empty space
            // So that alert bubbles persist after a simple tap, do not disappear when the finger leaves
            ((this._hoverObject && this._hoverObject != displayObject) || !interactionEvent.target)) {
            displayObject.___over = false;
            proto.dispatchEvent(displayObject, 'touchout', interactionEvent);
        }
    };
}
patchInteractionManager();