-- Catalogpage Oldpage Fix Class (after Catalogpage Oldpage Wrapper Class).
-- The wrapper's define reuses its product index i for the loop over a deal's contents, like Product Strip Base Class:
-- furni downloads registered after a deal carry the wrong #itemIndex, and its downloadCompleted then updates no
-- product (Trax: the jukebox after the sound machine deal kept the loading icon). Every completed furni download is
-- handed to the wrapper once for each product that still has no small preview.
on downloadCompleted me, tProps
  if tProps[#props].getaProp(#imagedownload) then
    return callAncestor(#downloadCompleted, [me], tProps)
  end if
  if ilk(me.pPageData) <> #propList then
    return callAncestor(#downloadCompleted, [me], tProps)
  end if
  tOffers = me.pPageData.getaProp(#offers)
  if ilk(tOffers) <> #propList then
    return callAncestor(#downloadCompleted, [me], tProps)
  end if
  tCount = tOffers.count
  repeat with tIndex = 1 to tCount
    if voidp(tOffers[tIndex].getSmallPreview()) then
      tItemProps = tProps[#props].duplicate()
      tItemProps.setaProp(#itemIndex, tIndex)
      tCopy = tProps.duplicate()
      tCopy.setaProp(#props, tItemProps)
      callAncestor(#downloadCompleted, [me], tCopy)
    end if
  end repeat
  return 1
end

-- The wrapper's define lets the old page class fill the window, then mergeWindow (feedPageData) blanks every
-- ctlg_text_N the page data has no text for, the pets page's name among them. A download that completes later
-- re-defines the page class (renderGridPreview), so the name only showed on a page's first visit. Define it again
-- once the window is merged.
on mergeWindow me, tParentWndObj
  tResult = callAncestor(#mergeWindow, [me], tParentWndObj)
  if objectp(me.pPageImplObj) then
    if me.pPageImplObj.handler(#define) then
      me.pPageImplObj.define(me.pOldPageData)
    end if
  end if
  return tResult
end

-- The wrapper refreshes icons with createTimeout(..., #renderGridPreview, me.getID()), and Fuse only creates a timeout
-- when the object's own script has the handler (handler() doesn't look at ancestors). With this class on top of the
-- chain, pass it on.
on renderGridPreview me, tItemIndex
  return callAncestor(#renderGridPreview, [me], tItemIndex)
end
