const countdownEl = document.getElementById("countdown");
const staminaEl = document.getElementById("res-stamina");
const appEl = document.querySelector(".app");
let homeCharacterId = null;
let homeSkinId = null;
let characterList = [];
let authMode = "login";
let cardPoolMap = new Map();

const target = Date.now() + 2 * 60 * 60 * 1000 + 15 * 60 * 1000 + 32 * 1000;

function pad(value) {
  return String(value).padStart(2, "0");
}

function tick() {
  const diff = Math.max(0, target - Date.now());
  const hours = Math.floor(diff / 3600000);
  const minutes = Math.floor((diff % 3600000) / 60000);
  const seconds = Math.floor((diff % 60000) / 1000);

  if (countdownEl) {
    countdownEl.textContent = `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
  }
}

function regenStamina() {
  if (!staminaEl) return;

  const [current, max] = staminaEl.textContent.split("/").map(Number);
  if (Number.isNaN(current) || Number.isNaN(max)) return;

  const next = current >= max ? max : current + 1;
  staminaEl.textContent = `${next}/${max}`;
}

function bindClick(selector, onClick) {
  document.querySelectorAll(selector).forEach((btn) => {
    btn.addEventListener("click", () => {
      const action = btn.dataset.action || btn.textContent.trim();
      onClick(btn, action);
    });
  });
}

function setAuthenticated(user) {
  if (appEl) {
    appEl.classList.toggle("is-authenticated", Boolean(user));
  }
  if (user) {
    const existing = getStoredUser();
    const normalized = {
      ...user,
      id: user.id ? String(user.id) : null,
      token: user.token || existing?.token || null,
    };
    localStorage.setItem("huyao_user", JSON.stringify(normalized));
    const nameEls = document.querySelectorAll(".player .name, .avatar-name");
    nameEls.forEach((el) => {
      el.textContent = user.username || "玩家";
    });
    updatePlayerLevel(normalized);
    const currentName = document.getElementById("account-current");
    if (currentName) {
      currentName.textContent = user.username || "玩家";
    }
  } else {
    localStorage.removeItem("huyao_user");
  }
}

function getStoredUser() {
  try {
    const raw = localStorage.getItem("huyao_user");
    const user = raw ? JSON.parse(raw) : null;
    return user && user.id ? { ...user, id: String(user.id) } : user;
  } catch {
    return null;
  }
}

function getAuthHeaders() {
  const user = getStoredUser();
  if (!user?.token) return {};
  return { Authorization: `Bearer ${user.token}` };
}

function showAuthExpired(message = "登录已过期，请重新登录") {
  setAuthenticated(null);
  const authPage = document.getElementById("auth-page");
  if (authPage) {
    authPage.classList.remove("is-register");
  }
  setAuthMode("login");
  const authMessage = document.getElementById("auth-message");
  if (authMessage) {
    authMessage.textContent = message;
  }
}

async function fetchWithAuth(url, options = {}) {
  const headers = { ...(options.headers || {}), ...getAuthHeaders() };
  const res = await fetch(url, { ...options, headers });
  const newToken = res.headers.get("X-Auth-Token");
  if (newToken) {
    const user = getStoredUser();
    if (user) {
      setAuthenticated({ ...user, token: newToken });
    }
  }
  if (res.status === 401) {
    showAuthExpired();
  }
  return res;
}

function updatePlayerLevel(user) {
  const levelEl = document.querySelector(".player .level");
  const barEl = document.querySelector(".player .xp-fill");
  if (!levelEl) return;
  const level = Number(user?.level ?? 1);
  const drawCount = Number(user?.drawCount ?? 0);
  const safeLevel = Number.isFinite(level) && level > 0 ? Math.min(100, level) : 1;
  levelEl.textContent = `Lv.${safeLevel}`;
  if (!barEl) return;
  if (!Number.isFinite(drawCount) || safeLevel >= 100) {
    barEl.style.width = "100%";
    return;
  }
  const currentRequired = 5 * (safeLevel - 1) * safeLevel;
  const nextRequired = 5 * safeLevel * (safeLevel + 1);
  const progress = Math.max(0, Math.min(1, (drawCount - currentRequired) / (nextRequired - currentRequired)));
  barEl.style.width = `${Math.round(progress * 100)}%`;
}

function openAccountSwitch() {
  const panel = document.getElementById("account-switch");
  if (panel) {
    panel.classList.add("active");
    panel.setAttribute("aria-hidden", "false");
  }
}

function closeAccountSwitch() {
  const panel = document.getElementById("account-switch");
  if (panel) {
    panel.classList.remove("active");
    panel.setAttribute("aria-hidden", "true");
  }
}

function setAuthMode(mode) {
  authMode = mode;
  const authPage = document.getElementById("auth-page");
  const authTitle = document.getElementById("auth-title");
  const authSubmit = document.getElementById("auth-submit");
  const authMessage = document.getElementById("auth-message");
  const authTabs = document.querySelectorAll(".auth-tab");
  const authPhoneField = document.getElementById("auth-phone-field");

  if (authPage) {
    authPage.classList.toggle("is-register", mode === "register");
  }
  if (authTitle) {
    authTitle.textContent = mode === "register" ? "注册" : "登录";
  }
  if (authSubmit) {
    authSubmit.textContent = mode === "register" ? "注册并登录" : "登录";
  }
  if (authMessage) {
    authMessage.textContent = "";
  }
  if (authPhoneField) {
    authPhoneField.style.display = mode === "register" ? "flex" : "none";
  }
  authTabs.forEach((tab) => {
    tab.classList.toggle("active", tab.dataset.auth === mode);
  });
}

async function submitAuth() {
  const username = document.getElementById("auth-username")?.value.trim();
  const password = document.getElementById("auth-password")?.value.trim();
  const phone = document.getElementById("auth-phone")?.value.trim();
  const authMessage = document.getElementById("auth-message");

  if (!username || !password || (authMode === "register" && !phone)) {
    if (authMessage) authMessage.textContent = "请填写完整信息";
    return;
  }

  const endpoint = authMode === "register" ? "/api/auth/register" : "/api/auth/login";
  const payload = authMode === "register"
    ? { username, password, phonenumber: phone }
    : { username, password };

  try {
    const res = await fetch(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const error = await res.text();
      throw new Error(error || "请求失败");
    }
    const data = await res.json();
    if (data && data.user && data.token) {
      setAuthenticated({ ...data.user, token: data.token });
    } else {
      setAuthenticated(data);
    }
  } catch (err) {
    if (authMessage) authMessage.textContent = err.message || "登录失败";
  }
}

bindClick(".left-menu .menu-item", (_, action) => {
  console.log(`left-menu click: ${action}`);
});

function activateInRightBoard(target) {
  document.querySelectorAll(".right-board .is-active").forEach((item) => {
    item.classList.remove("is-active");
  });
  target.classList.add("is-active");
}

bindClick(".event-card, .right-board .banner, .right-board .notice", (btn, action) => {
  activateInRightBoard(btn);
  if (action === "gacha") {
    openSummonPage();
  } else if (action === "gallery") {
    openGalleryPage();
  } else {
    console.log(`right-board click: ${action}`);
  }
});

bindClick(".bottombar .tab", (btn, action) => {
  document.querySelectorAll(".bottombar .tab").forEach((item) => {
    item.classList.remove("active");
  });
  btn.classList.add("active");
  if (action === "role") {
    openCharacterPage();
  } else {
    closeCharacterDetail();
    closeCharacterPage();
  }
  console.log(`bottom-tab click: ${action}`);
});

bindClick(".resource .add", (_, action) => {
  console.log(`resource add click: ${action}`);
});

bindClick(".top-left", (_, action) => {
  if (!appEl?.classList.contains("is-authenticated")) return;
  document.querySelectorAll(".top-left.is-active").forEach((item) => {
    item.classList.remove("is-active");
  });
  document.querySelectorAll(".resource.is-active").forEach((item) => {
    item.classList.remove("is-active");
  });
  document.querySelector(".top-left")?.classList.add("is-active");
  openAvatarPage();
  console.log(`profile click: ${action}`);
});

bindClick(".resources .resource", (_, action) => {
  if (!appEl?.classList.contains("is-authenticated")) return;
  document.querySelectorAll(".top-left.is-active, .resource.is-active").forEach((item) => {
    item.classList.remove("is-active");
  });
  const target = document.querySelector(`.resources .resource[data-action="${action}"]`);
  if (target) {
    target.classList.add("is-active");
  }
  console.log(`resource click: ${action}`);
});


function buildCharacterGridFromList(list) {
  const grid = document.querySelector(".character-grid");
  if (!grid) return;
  grid.innerHTML = "";

  list.forEach((item) => {
    const card = document.createElement("div");
    card.className = "character-card";
    card.dataset.character = String(item.id);
    card.dataset.image = item.image;
    card.dataset.name = String(item.name || item.id);
    card.dataset.quote = item.quote || "";

    const img = new Image();
    img.src = item.image;
    img.alt = `角色${item.id}`;

    const name = document.createElement("div");
    name.className = "character-name";
    name.textContent = String(item.name || item.id);

    card.appendChild(img);
    card.appendChild(name);
    grid.appendChild(card);
  });
}

function buildGalleryGrid(list) {
  const grid = document.getElementById("gallery-grid");
  if (!grid) return;
  grid.innerHTML = "";
  list.forEach((item) => {
    const card = document.createElement("div");
    card.className = "character-card";
    card.dataset.character = String(item.id);
    card.dataset.image = item.image;
    card.dataset.name = String(item.name || item.id);
    card.dataset.quote = item.quote || "";

    const img = new Image();
    img.src = item.image;
    img.alt = `角色${item.id}`;

    const name = document.createElement("div");
    name.className = "character-name";
    name.textContent = String(item.name || item.id);

    card.appendChild(img);
    card.appendChild(name);
    grid.appendChild(card);
  });
}

function buildCharacterGridFallback() {
  const grid = document.querySelector(".character-grid");
  if (!grid) return;

  const base = grid.dataset.base || "images/character/character";
  const ext = grid.dataset.ext || ".jpg";
  let index = 1;

  function tryLoad() {
    const img = new Image();
    const src = `${base}${index}${ext}`;
    img.src = src;
    img.alt = `角色${index}`;

    img.onload = () => {
      const card = document.createElement("div");
      card.className = "character-card";
      card.dataset.character = String(index);
      card.dataset.image = src;
      card.dataset.name = String(index);
      card.dataset.quote = "";

      const name = document.createElement("div");
      name.className = "character-name";
      name.textContent = String(index);

      card.appendChild(img);
      card.appendChild(name);
      grid.appendChild(card);

      index += 1;
      tryLoad();
    };

    img.onerror = () => {
      // Stop when the next sequential image is missing.
    };
  }

  tryLoad();
}

async function loadCharacters() {
  try {
    const res = await fetchWithAuth("/api/characters");
    if (!res.ok) throw new Error("bad response");
    const list = await res.json();
    if (!Array.isArray(list)) throw new Error("invalid payload");
    characterList = list;
    const ownedIds = await loadOwnedCharacterIds();
    const visible = ownedIds ? list.filter((item) => ownedIds.has(String(item.id))) : list;
    buildCharacterGridFromList(visible);
    buildGalleryGrid(list);
    await loadHomeCharacter();
    await loadCardPools();
  } catch (err) {
    buildCharacterGridFallback();
  }
}

document.addEventListener("DOMContentLoaded", loadCharacters);

function getCharacterImage(characterId) {
  const match = characterList.find((item) => String(item.id) === String(characterId));
  return match ? match.image : `images/character/character${characterId}.jpg`;
}

async function loadOwnedCharacterIds() {
  const userId = getStoredUserId();
  if (!userId) return null;
  try {
    const res = await fetchWithAuth(`/api/user-characters?userId=${encodeURIComponent(userId)}`);
    if (!res.ok) return null;
    const list = await res.json();
    if (!Array.isArray(list)) return null;
    if (list.length === 0) {
      await fetchWithAuth("/api/user-characters/bootstrap", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId,
          names: ["character1"],
        }),
      });
      const retry = await fetchWithAuth(`/api/user-characters?userId=${encodeURIComponent(userId)}`);
      if (!retry.ok) return null;
      const retryList = await retry.json();
      if (!Array.isArray(retryList)) return null;
      return new Set(retryList.map((item) => String(item.characterId)));
    }
    return new Set(list.map((item) => String(item.characterId)));
  } catch {
    return null;
  }
}

function getStoredUserId() {
  const user = getStoredUser();
  return user?.id ? String(user.id) : null;
}

async function drawCharacter(times = 1) {
  if (!appEl?.classList.contains("is-authenticated")) {
    const authMessage = document.getElementById("auth-message");
    if (authMessage) authMessage.textContent = "请先登录";
    return;
  }
  const userId = getStoredUserId();
  if (!userId) return;

  try {
    const results = [];
    for (let i = 0; i < times; i += 1) {
      const res = await fetchWithAuth("/api/user-characters/draw", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userId }),
      });
      if (!res.ok) {
        throw new Error("抽卡失败");
      }
      results.push(await res.json());
    }
    await refreshUserStats(userId);
    const resultEl = document.getElementById("summon-result");
    const grid = document.getElementById("summon-grid");
    if (times === 10 && grid) {
      grid.innerHTML = "";
      grid.classList.add("active");
      const summonPage = document.getElementById("summon-page");
      if (summonPage) {
        summonPage.classList.add("show-grid");
      }
      results.forEach((item) => {
        const hit = item?.hit;
        const card = document.createElement("div");
        card.className = "summon-item";
        if (hit && item.character) {
          const img = new Image();
          img.src = item.character.image;
          img.alt = item.character.name;
          const name = document.createElement("div");
          name.className = "summon-item-name";
          name.textContent = item.character.name;
          card.appendChild(img);
          card.appendChild(name);
        } else {
          const placeholder = document.createElement("div");
          placeholder.className = "summon-item-name";
          placeholder.textContent = "物品1";
          card.appendChild(placeholder);
        }
        grid.appendChild(card);
      });
      await loadCharacters();
      if (resultEl) resultEl.textContent = "";
      return;
    }

    if (grid) {
      grid.classList.remove("active");
      grid.innerHTML = "";
    }
    const last = results[results.length - 1];
    const character = last?.character;
    if (character) {
      if (resultEl) resultEl.textContent = `获得：${character.name}`;
      openCharacterDetail(character.id, {
        image: character.image,
        name: character.name,
      });
    } else if (resultEl) {
      resultEl.textContent = "获得：物品1";
    }
    await loadCharacters();
  } catch {
    console.log("draw failed");
  }
}

async function refreshUserStats(userId) {
  try {
    const res = await fetchWithAuth(`/api/users/${encodeURIComponent(userId)}`);
    if (!res.ok) return;
    const user = await res.json();
    if (user && user.id) {
      setAuthenticated(user);
    } else {
      updatePlayerLevel(user);
    }
  } catch {
    // ignore
  }
}

async function loadHomeCharacter() {
  try {
    const res = await fetchWithAuth("/api/home-character");
    if (!res.ok) return;
    const data = await res.json();
    if (!data || !data.characterId) return;
    const homeIllustration = document.querySelector(".illustration");
    const speech = document.querySelector(".speech");
    if (homeIllustration) {
      homeIllustration.src = data.skinImage || getCharacterImage(data.characterId);
    }
    homeCharacterId = String(data.characterId);
    homeSkinId = data.skinId ? String(data.skinId) : null;
    if (speech && data.quote) {
      speech.textContent = data.quote;
    }
  } catch (err) {
    // ignore
  }
}

function openCharacterDetail(characterId, options = {}) {
  const detail = document.getElementById("character-detail");
  const illustration = document.getElementById("detail-illustration");
  const name = document.getElementById("detail-name");
  const likeButton = document.querySelector(".like-button");
  const skinList = document.getElementById("detail-skins");
  const ultimateSlot = document.getElementById("detail-ultimate");
  const skill1Slot = document.getElementById("detail-skill1");
  const skill2Slot = document.getElementById("detail-skill2");

  if (!detail || !illustration || !name || !likeButton) return;

  if (options.fromGallery) {
    closeGalleryPage();
  }
  likeButton.style.display = options.hideLike ? "none" : "inline-flex";
  detail.dataset.current = String(characterId);
  detail.dataset.skinId = "";
  const image = options.image || getCharacterImage(characterId);
  const title = options.name || `角色 ${characterId}`;
  illustration.src = image;
  illustration.alt = title;
  name.textContent = title;
  if (skinList) skinList.innerHTML = "";
  if (ultimateSlot) ultimateSlot.innerHTML = "";
  if (skill1Slot) skill1Slot.innerHTML = "";
  if (skill2Slot) skill2Slot.innerHTML = "";
  loadCharacterSkins(characterId, image);
  loadCharacterSkills(characterId);

  if (!options.hideLike) {
    updateLikeStatus(detail, characterId);
  }

  detail.classList.add("active");
  detail.setAttribute("aria-hidden", "false");
}

async function loadCharacterSkills(characterId) {
  const ultimateSlot = document.getElementById("detail-ultimate");
  const skill1Slot = document.getElementById("detail-skill1");
  const skill2Slot = document.getElementById("detail-skill2");
  if (!ultimateSlot || !skill1Slot || !skill2Slot) return;
  try {
    const res = await fetchWithAuth(
      `/api/character-skills?characterId=${encodeURIComponent(characterId)}`
    );
    if (!res.ok) return;
    const list = await res.json();
    if (!Array.isArray(list) || list.length === 0) return;
    const map = new Map(list.map((item) => [item.skillKey, item.image]));
    renderSkillImage(ultimateSlot, map.get("ultimate"));
    renderSkillImage(skill1Slot, map.get("skill1"));
    renderSkillImage(skill2Slot, map.get("skill2"));
  } catch {
    // ignore
  }
}

function renderSkillImage(slot, imageUrl) {
  slot.innerHTML = "";
  if (!imageUrl) return;
  const img = new Image();
  img.src = imageUrl;
  img.alt = "";
  slot.appendChild(img);
}

async function loadCharacterSkins(characterId, currentImage) {
  const skinList = document.getElementById("detail-skins");
  const illustration = document.getElementById("detail-illustration");
  const detail = document.getElementById("character-detail");
  if (!skinList || !illustration) return;
  try {
    const res = await fetchWithAuth(
      `/api/character-skins?characterId=${encodeURIComponent(characterId)}`
    );
    if (!res.ok) return;
    const list = await res.json();
    if (!Array.isArray(list) || list.length === 0) return;
    const sorted = [...list].sort((a, b) => {
      if (a.isDefault && !b.isDefault) return -1;
      if (!a.isDefault && b.isDefault) return 1;
      return String(a.skinKey || "").localeCompare(String(b.skinKey || ""));
    });
    const initialImage = currentImage || illustration.src;
    let activeSet = false;
    sorted.forEach((skin) => {
      const btn = document.createElement("button");
      btn.className = "skin-item";
      btn.type = "button";
      btn.dataset.image = skin.image;
      btn.dataset.skin = skin.skinKey || "";
      btn.dataset.skinId = skin.id;
      const img = new Image();
      img.src = skin.image;
      img.alt = skin.name || "皮肤";
      btn.appendChild(img);
      btn.addEventListener("click", () => {
        illustration.src = skin.image;
        skinList.querySelectorAll(".skin-item").forEach((item) => {
          item.classList.remove("active");
        });
        btn.classList.add("active");
        if (detail) {
          detail.dataset.skinId = String(skin.id || "");
          updateLikeStatus(detail, characterId);
        }
      });
      if (!activeSet && (skin.image === initialImage || skin.isDefault)) {
        btn.classList.add("active");
        if (detail) {
          detail.dataset.skinId = String(skin.id || "");
          updateLikeStatus(detail, characterId);
        }
        activeSet = true;
      }
      skinList.appendChild(btn);
    });
  } catch {
    // ignore
  }
}

function updateLikeStatus(detail, characterId) {
  const likeButton = document.querySelector(".like-button");
  if (!likeButton || !detail) return;
  const currentSkinId = detail.dataset.skinId || "";
  const isActive = homeCharacterId === String(characterId)
    && (homeSkinId ? String(homeSkinId) === String(currentSkinId) : false);
  likeButton.classList.toggle("is-active", isActive);
  likeButton.textContent = isActive ? "♥" : "♡";
}

function closeCharacterDetail() {
  const detail = document.getElementById("character-detail");
  if (!detail) return;
  detail.classList.remove("active");
  detail.setAttribute("aria-hidden", "true");
}

function openCharacterPage() {
  const page = document.getElementById("character-page");
  if (!page) return;
  page.classList.add("active");
  page.setAttribute("aria-hidden", "false");
}

function closeCharacterPage() {
  const page = document.getElementById("character-page");
  if (!page) return;
  page.classList.remove("active");
  page.setAttribute("aria-hidden", "true");
}

function openGalleryPage() {
  const page = document.getElementById("gallery-page");
  if (!page) return;
  page.classList.add("active");
  page.setAttribute("aria-hidden", "false");
}

function closeGalleryPage() {
  const page = document.getElementById("gallery-page");
  if (!page) return;
  page.classList.remove("active");
  page.setAttribute("aria-hidden", "true");
}

function openSummonPage() {
  const page = document.getElementById("summon-page");
  if (!page) return;
  page.classList.add("active");
  page.setAttribute("aria-hidden", "false");
  applyActivePoolBanner();
}

function closeSummonPage() {
  const page = document.getElementById("summon-page");
  if (!page) return;
  page.classList.remove("active");
  page.setAttribute("aria-hidden", "true");
}

function openAvatarPage() {
  const page = document.getElementById("avatar-page");
  if (!page) return;
  closeCharacterDetail();
  page.classList.add("active");
  page.setAttribute("aria-hidden", "false");
}

function closeAvatarPage() {
  const page = document.getElementById("avatar-page");
  if (!page) return;
  page.classList.remove("active");
  page.setAttribute("aria-hidden", "true");
}

document.addEventListener("click", (event) => {
  const card = event.target.closest(".character-card");
  if (card && card.dataset.character) {
    openCharacterDetail(card.dataset.character, {
      image: card.dataset.image,
      name: card.dataset.name,
      hideLike: card.closest("#gallery-grid") !== null,
      fromGallery: card.closest("#gallery-grid") !== null,
    });
  }
});

bindClick('.back-button[data-action="back-character"]', () => {
  closeCharacterDetail();
  console.log("back to character page");
});

bindClick('.like-button[data-action="set-home"]', async () => {
  const detail = document.getElementById("character-detail");
  const current = detail?.dataset.current;
  let skinId = detail?.dataset.skinId;
  if (!skinId) {
    const activeSkin = detail?.querySelector(".skin-item.active");
    skinId = activeSkin?.dataset?.skinId || "";
  }
  if (!appEl?.classList.contains("is-authenticated")) {
    showAuthExpired("请先登录");
    return;
  }
  if (!current) return;
  const homeIllustration = document.querySelector(".illustration");
  try {
    const res = await fetchWithAuth("/api/home-character", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ characterId: String(current), skinId: skinId || null }),
    });
    if (!res.ok) {
      if (res.status === 401) {
        showAuthExpired();
        return;
      }
      throw new Error(await res.text());
    }
    const data = await res.json();
    if (homeIllustration) {
      homeIllustration.src = data?.skinImage || getCharacterImage(current);
    }
    const speech = document.querySelector(".speech");
    if (speech && data?.quote) {
      speech.textContent = data.quote;
    }
    homeCharacterId = String(current);
    homeSkinId = data?.skinId ? String(data.skinId) : null;
    if (detail && data?.skinId) {
      detail.dataset.skinId = String(data.skinId);
    }
    updateLikeStatus(detail, current);
    console.log(`set home character: ${current}`);
  } catch (err) {
    console.log("set home character failed", err?.message || err);
  }
});

bindClick('.back-button[data-action="back-home"]', () => {
  closeCharacterDetail();
  closeCharacterPage();
  closeGalleryPage();
  closeSummonPage();
  closeAvatarPage();
  const homeTab = document.querySelector('.bottombar .tab[data-action="home"]');
  if (homeTab) {
    document.querySelectorAll(".bottombar .tab").forEach((item) => {
      item.classList.remove("active");
    });
    homeTab.classList.add("active");
  }
  console.log("back to home");
});

bindClick('.back-button[data-action="back-avatar"]', () => {
  closeAvatarPage();
  console.log("back from avatar page");
});

bindClick('.back-button[data-action="back-gallery"]', () => {
  closeGalleryPage();
  console.log("back from gallery");
});

bindClick('.back-button[data-action="back-summon"]', () => {
  closeSummonPage();
  console.log("back from summon");
});

bindClick(".avatar-option", (btn) => {
  if (!appEl?.classList.contains("is-authenticated")) return;
  document.querySelectorAll(".avatar-option").forEach((item) => {
    item.classList.remove("is-active");
  });
  btn.classList.add("is-active");

  const color = btn.dataset.color || "#ffc6b0";
  const avatarLarge = document.getElementById("avatar-large");
  const avatarSmall = document.querySelector(".top-left .avatar");
  if (avatarLarge) {
    avatarLarge.style.background = color;
  }
  if (avatarSmall) {
    avatarSmall.style.background = color;
  }
});

tick();
setInterval(tick, 1000);
setInterval(regenStamina, 4000);

document.addEventListener("DOMContentLoaded", () => {
  const stored = getStoredUser();
  if (stored) {
    setAuthenticated(stored);
  }

  bindClick(".auth-tab", (btn) => {
    setAuthMode(btn.dataset.auth || "login");
  });

  document.getElementById("auth-submit")?.addEventListener("click", submitAuth);

  setAuthMode("login");

  document.getElementById("switch-account")?.addEventListener("click", () => {
    openAccountSwitch();
  });
  document.getElementById("account-cancel")?.addEventListener("click", () => {
    closeAccountSwitch();
  });
  document.getElementById("account-to-auth")?.addEventListener("click", () => {
    closeAccountSwitch();
    setAuthenticated(null);
    const authPage = document.getElementById("auth-page");
    if (authPage) {
      authPage.classList.remove("is-register");
    }
    setAuthMode("login");
  });

  document.getElementById("summon-one")?.addEventListener("click", () => {
    drawCharacter(1);
  });
  document.getElementById("summon-ten")?.addEventListener("click", () => {
    drawCharacter(10);
  });

  document.getElementById("summon-grid")?.addEventListener("click", () => {
    const grid = document.getElementById("summon-grid");
    const summonPage = document.getElementById("summon-page");
    if (grid) {
      grid.classList.remove("active");
      grid.innerHTML = "";
    }
    if (summonPage) {
      summonPage.classList.remove("show-grid");
    }
    closeSummonPage();
  });

  document.querySelectorAll(".summon-tab").forEach((tab) => {
    tab.addEventListener("click", () => {
      document.querySelectorAll(".summon-tab").forEach((item) => {
        item.classList.remove("active");
      });
      tab.classList.add("active");
      applyActivePoolBanner(tab);
    });
  });
});

async function loadCardPools() {
  try {
    const res = await fetchWithAuth("/api/card-pools");
    if (!res.ok) return;
    const list = await res.json();
    if (!Array.isArray(list)) return;
    cardPoolMap = new Map(list.map((item) => [item.poolKey, item]));
    applyActivePoolBanner();
  } catch {
    // ignore
  }
}

function applyActivePoolBanner(activeTab) {
  const tab = activeTab || document.querySelector(".summon-tab.active");
  const banner = document.querySelector(".summon-banner");
  const titleEl = document.querySelector(".summon-banner-title");
  const subEl = document.querySelector(".summon-banner-sub");
  if (!tab || !banner) return;
  const poolKey = tab.dataset.pool || "pool1";
  const pool = cardPoolMap.get(poolKey);
  if (titleEl) {
    titleEl.textContent = pool?.name || tab.textContent || "常驻卡池";
  }
  if (subEl) {
    subEl.textContent = "获取稀有角色与道具";
  }
  banner.style.backgroundImage = pool?.image ? `url("${pool.image}")` : "";
}
