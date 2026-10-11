#pragma once
#include <cmath>

// Sign-corrected quaternion midpoint; positions and FOV are never modified.
template<class Q> bool parallelMidpoint(const Q& a, const Q& b, Q& out) {
    float sign = (a.x*b.x + a.y*b.y + a.z*b.z + a.w*b.w) < 0 ? -1.0f : 1.0f;
    Q q = {a.x+sign*b.x, a.y+sign*b.y, a.z+sign*b.z, a.w+sign*b.w};
    float n = std::sqrt(q.x*q.x + q.y*q.y + q.z*q.z + q.w*q.w);
    if (!std::isfinite(n) || n <= 0.00001f) return false;
    q.x /= n; q.y /= n; q.z /= n; q.w /= n;
    out = q;
    return true;
}
